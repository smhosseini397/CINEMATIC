import { computeMotionTransform, MotionStyleType } from './webMotionEngine';

export interface RenderProgressUpdate {
  isRendering: boolean;
  currentFrame: number;
  totalFrames: number;
  percentage: number;
  elapsedMs: number;
  remainingMs: number;
  statusText: string;
}

export async function renderVideoFromImage(
  sourceCanvas: HTMLCanvasElement,
  style: MotionStyleType,
  options: {
    durationSeconds: number;
    fps: 30 | 60;
    resolution: { width: number; height: number };
    enableLighting: boolean;
  },
  onProgress: (progress: RenderProgressUpdate) => void
): Promise<Blob> {
  const { durationSeconds, fps, resolution, enableLighting } = options;
  const totalFrames = durationSeconds * fps;
  const frameIntervalMs = 1000 / fps;

  // Offscreen rendering canvas matching target export resolution
  const renderCanvas = document.createElement('canvas');
  renderCanvas.width = resolution.width;
  renderCanvas.height = resolution.height;
  const ctx = renderCanvas.getContext('2d', { alpha: false });
  if (!ctx) throw new Error('Could not obtain 2D rendering context');

  // MediaRecorder stream
  const stream = renderCanvas.captureStream(fps);
  const mimeType = MediaRecorder.isTypeSupported('video/mp4;codecs=avc1')
    ? 'video/mp4;codecs=avc1'
    : MediaRecorder.isTypeSupported('video/webm;codecs=h264')
    ? 'video/webm;codecs=h264'
    : MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
    ? 'video/webm;codecs=vp9'
    : 'video/webm';

  const mediaRecorder = new MediaRecorder(stream, {
    mimeType,
    videoBitsPerSecond: resolution.width >= 3840 ? 45_000_000 : 22_000_000
  });

  const chunks: Blob[] = [];
  mediaRecorder.ondataavailable = (e) => {
    if (e.data && e.data.size > 0) chunks.push(e.data);
  };

  const renderCompletePromise = new Promise<Blob>((resolve) => {
    mediaRecorder.onstop = () => {
      const blob = new Blob(chunks, { type: mimeType });
      resolve(blob);
    };
  });

  mediaRecorder.start(100);

  const startTime = performance.now();

  for (let frame = 0; frame < totalFrames; frame++) {
    const progress = frame / Math.max(1, totalFrames - 1);
    const transform = computeMotionTransform(style, progress);

    // Draw frame onto renderCanvas
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, resolution.width, resolution.height);

    ctx.save();
    const cx = resolution.width / 2;
    const cy = resolution.height / 2;

    const scaleFit = Math.max(
      resolution.width / sourceCanvas.width,
      resolution.height / sourceCanvas.height
    );

    ctx.translate(
      cx + transform.transX * resolution.width,
      cy + transform.transY * resolution.height
    );
    ctx.scale(scaleFit * transform.scale, scaleFit * transform.scale);
    ctx.rotate((transform.rotationZ * Math.PI) / 180);

    // Draw source canvas without altering original colors
    ctx.drawImage(
      sourceCanvas,
      -sourceCanvas.width / 2,
      -sourceCanvas.height / 2,
      sourceCanvas.width,
      sourceCanvas.height
    );

    ctx.restore();

    // Studio directional grazing lighting simulation
    if (enableLighting && transform.lightIntensity > 0.01) {
      const rad = (transform.lightAngle * Math.PI) / 180;
      const lx = cx + Math.cos(rad) * cx * 0.75;
      const ly = cy + Math.sin(rad) * cy * 0.75;
      const radius = Math.max(resolution.width, resolution.height) * 0.65;

      const gradient = ctx.createRadialGradient(lx, ly, 0, lx, ly, radius);
      gradient.addColorStop(0, `rgba(255, 248, 235, ${transform.lightIntensity})`);
      gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');

      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, resolution.width, resolution.height);
    }

    // Force frame capture in captureStream
    const track = stream.getVideoTracks()[0] as any;
    if (track && typeof track.requestFrame === 'function') {
      track.requestFrame();
    }

    const elapsed = performance.now() - startTime;
    const avgPerFrame = elapsed / (frame + 1);
    const remaining = (totalFrames - (frame + 1)) * avgPerFrame;

    onProgress({
      isRendering: true,
      currentFrame: frame + 1,
      totalFrames,
      percentage: Math.round(((frame + 1) / totalFrames) * 100),
      elapsedMs: Math.round(elapsed),
      remainingMs: Math.round(remaining),
      statusText: `MediaCodec Surface Processing · Frame ${frame + 1} of ${totalFrames}`
    });

    // Small yield for smooth UI animation and stream ingestion
    await new Promise((r) => setTimeout(r, Math.max(2, frameIntervalMs * 0.2)));
  }

  mediaRecorder.stop();
  return renderCompletePromise;
}
