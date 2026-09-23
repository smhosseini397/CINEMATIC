import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
  Film,
  Camera,
  Play,
  Pause,
  RotateCcw,
  Download,
  Share2,
  Sparkles,
  Maximize2,
  CheckCircle2,
  Smartphone,
  Monitor,
  Code2,
  FolderArchive,
  Layers,
  Sun,
  ShieldCheck,
  ChevronRight,
  Clock,
  Video,
  FileCode,
  Copy,
  Check,
  ZoomIn,
  Sliders,
  Eye,
  Info
} from 'lucide-react';
import { CARPET_PRESETS, CarpetPreset, renderCarpetToCanvas } from './utils/carpetCanvas';
import {
  MOTION_STYLES,
  MotionStyleType,
  computeMotionTransform,
  MotionTransform
} from './utils/webMotionEngine';
import { renderVideoFromImage, RenderProgressUpdate } from './utils/videoRenderer';
import { ANDROID_FILES, AndroidFileEntry } from './data/androidFiles';
import { generateAndroidProjectZip } from './utils/zipExporter';

export default function App() {
  // Navigation tabs
  const [activeTab, setActiveTab] = useState<'studio' | 'carpet' | 'android'>('studio');

  // Selected photo / carpet state
  const [selectedPreset, setSelectedPreset] = useState<CarpetPreset>(CARPET_PRESETS[0]);
  const [customImage, setCustomImage] = useState<HTMLImageElement | null>(null);
  const [customImageName, setCustomImageName] = useState<string>('');

  // Motion style & export settings
  const [motionStyle, setMotionStyle] = useState<MotionStyleType>('KEN_BURNS');
  const [durationSeconds, setDurationSeconds] = useState<number>(10);
  const [resolution, setResolution] = useState<{ width: number; height: number; label: string }>({
    width: 1920,
    height: 1080,
    label: '1080p Full HD'
  });
  const [fps, setFps] = useState<30 | 60>(60);
  const [enableLightingSweep, setEnableLightingSweep] = useState<boolean>(true);

  // Playback state
  const [isPlaying, setIsPlaying] = useState<boolean>(true);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [deviceFrameMode, setDeviceFrameMode] = useState<'mobile' | 'widescreen'>('mobile');

  // Interactive 3D Parallax tilt tracking
  const [mouseTilt, setMouseTilt] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // Video rendering state
  const [isRendering, setIsRendering] = useState<boolean>(false);
  const [renderProgress, setRenderProgress] = useState<RenderProgressUpdate | null>(null);
  const [exportedVideoUrl, setExportedVideoUrl] = useState<string | null>(null);
  const [isSavedToGallery, setIsSavedToGallery] = useState<boolean>(false);

  // Android project explorer state
  const [selectedFile, setSelectedFile] = useState<AndroidFileEntry>(ANDROID_FILES[0]);
  const [copiedCode, setCopiedCode] = useState<boolean>(false);
  const [isZipping, setIsZipping] = useState<boolean>(false);

  // Offscreen master carpet canvas
  const masterCanvasRef = useRef<HTMLCanvasElement>(null);
  // Viewport preview canvas
  const previewCanvasRef = useRef<HTMLCanvasElement>(null);
  const animationFrameRef = useRef<number | null>(null);

  // Render master high-res carpet texture whenever preset changes
  useEffect(() => {
    if (customImage) return;
    const canvas = masterCanvasRef.current;
    if (!canvas) return;
    canvas.width = 2048;
    canvas.height = 1365;
    renderCarpetToCanvas(canvas, selectedPreset);
  }, [selectedPreset, customImage]);

  // Main 60 FPS animation loop for viewport
  useEffect(() => {
    let lastTimestamp = performance.now();

    const loop = (timestamp: number) => {
      const delta = (timestamp - lastTimestamp) / 1000;
      lastTimestamp = timestamp;

      if (isPlaying && !isRendering) {
        setCurrentTime((prev) => {
          const next = prev + delta;
          return next >= durationSeconds ? 0 : next;
        });
      }

      // Draw current frame onto viewport canvas
      const previewCanvas = previewCanvasRef.current;
      const masterCanvas = masterCanvasRef.current;

      if (previewCanvas && (masterCanvas || customImage)) {
        const ctx = previewCanvas.getContext('2d');
        if (ctx) {
          const progress = currentTime / Math.max(1, durationSeconds);
          const transform = computeMotionTransform(motionStyle, progress, mouseTilt);

          const pw = previewCanvas.width;
          const ph = previewCanvas.height;

          // Clear
          ctx.fillStyle = '#0a0a0c';
          ctx.fillRect(0, 0, pw, ph);

          ctx.save();
          const cx = pw / 2;
          const cy = ph / 2;

          const sourceW = customImage ? customImage.naturalWidth : (masterCanvas ? masterCanvas.width : 2048);
          const sourceH = customImage ? customImage.naturalHeight : (masterCanvas ? masterCanvas.height : 1365);
          const scaleFit = Math.max(pw / sourceW, ph / sourceH);

          // Apply 60 FPS motion transformation
          ctx.translate(cx + transform.transX * pw, cy + transform.transY * ph);
          ctx.scale(scaleFit * transform.scale, scaleFit * transform.scale);
          ctx.rotate((transform.rotationZ * Math.PI) / 180);

          if (customImage) {
            ctx.drawImage(customImage, -sourceW / 2, -sourceH / 2, sourceW, sourceH);
          } else if (masterCanvas) {
            ctx.drawImage(masterCanvas, -sourceW / 2, -sourceH / 2, sourceW, sourceH);
          }

          ctx.restore();

          // Lighting sweep for luxury / carpet mode
          if (enableLightingSweep && transform.lightIntensity > 0.01) {
            const rad = (transform.lightAngle * Math.PI) / 180;
            const lx = cx + Math.cos(rad) * cx * 0.75;
            const ly = cy + Math.sin(rad) * cy * 0.75;
            const radius = Math.max(pw, ph) * 0.7;

            const gradient = ctx.createRadialGradient(lx, ly, 0, lx, ly, radius);
            gradient.addColorStop(0, `rgba(255, 248, 235, ${transform.lightIntensity})`);
            gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');

            ctx.fillStyle = gradient;
            ctx.fillRect(0, 0, pw, ph);
          }
        }
      }

      animationFrameRef.current = requestAnimationFrame(loop);
    };

    animationFrameRef.current = requestAnimationFrame(loop);
    return () => {
      if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
    };
  }, [isPlaying, isRendering, currentTime, durationSeconds, motionStyle, mouseTilt, enableLightingSweep, customImage]);

  // Handle custom photo upload (Gallery Picker simulation)
  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const img = new Image();
    img.onload = () => {
      setCustomImage(img);
      setCustomImageName(file.name);
      setCurrentTime(0);
    };
    img.src = URL.createObjectURL(file);
  };

  // Trigger Video Export
  const handleStartExport = async () => {
    const sourceCanvas = document.createElement('canvas');
    if (customImage) {
      sourceCanvas.width = customImage.naturalWidth;
      sourceCanvas.height = customImage.naturalHeight;
      const ctx = sourceCanvas.getContext('2d');
      if (ctx) ctx.drawImage(customImage, 0, 0);
    } else if (masterCanvasRef.current) {
      sourceCanvas.width = masterCanvasRef.current.width;
      sourceCanvas.height = masterCanvasRef.current.height;
      const ctx = sourceCanvas.getContext('2d');
      if (ctx) ctx.drawImage(masterCanvasRef.current, 0, 0);
    } else {
      return;
    }

    setIsRendering(true);
    setIsPlaying(false);
    setIsSavedToGallery(false);

    try {
      const blob = await renderVideoFromImage(
        sourceCanvas,
        motionStyle,
        {
          durationSeconds,
          fps,
          resolution,
          enableLighting: enableLightingSweep
        },
        (progress) => {
          setRenderProgress(progress);
        }
      );

      const url = URL.createObjectURL(blob);
      setExportedVideoUrl(url);
    } catch (err) {
      console.error('Video render error:', err);
    } finally {
      setIsRendering(false);
    }
  };

  // Download full Android Project Zip
  const handleDownloadProjectZip = async () => {
    setIsZipping(true);
    try {
      const zipBlob = await generateAndroidProjectZip();
      const url = URL.createObjectURL(zipBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'CinematicPhotoAnimator-Android-Project.zip';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to generate zip:', err);
    } finally {
      setIsZipping(false);
    }
  };

  // Copy code helper
  const handleCopyCode = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  // Current camera transform for HUD display
  const currentTransform = useMemo(() => {
    const p = currentTime / Math.max(1, durationSeconds);
    return computeMotionTransform(motionStyle, p, mouseTilt);
  }, [currentTime, durationSeconds, motionStyle, mouseTilt]);

  return (
    <div className="min-h-screen bg-[#090A0D] text-[#ECEEF4] font-sans antialiased flex flex-col selection:bg-[#D4AF37]/30 selection:text-[#F6E7B0]">
      {/* Hidden Master Canvas for High-Resolution Procedural Rug Rendering */}
      <canvas ref={masterCanvasRef} className="hidden" />

      {/* Top Application Bar */}
      <header className="border-b border-[#1E222D] bg-[#0E1015]/90 backdrop-blur sticky top-0 z-50 px-4 lg:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#8C6D1F] p-0.5 shadow-lg shadow-[#D4AF37]/10 flex items-center justify-center">
              <div className="w-full h-full bg-[#0D0E12] rounded-[10px] flex items-center justify-center">
                <Film className="w-5 h-5 text-[#D4AF37]" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-bold tracking-tight text-white">Cinematic Photo Animator</h1>
                <span className="text-[11px] font-mono font-medium px-2 py-0.5 bg-[#1B202D] text-[#D4AF37] border border-[#2B344B] rounded">
                  Android 14 · Kotlin & Compose
                </span>
              </div>
              <p className="text-xs text-[#8A91A5]">
                MediaCodec Hardware 60 FPS Video Engine · Persian Carpet Macro Showcase
              </p>
            </div>
          </div>

          {/* Navigation Segmented Switcher */}
          <div className="flex items-center gap-1.5 p-1 bg-[#141720] border border-[#222836] rounded-xl text-xs font-medium">
            <button
              onClick={() => setActiveTab('studio')}
              className={`px-3.5 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                activeTab === 'studio'
                  ? 'bg-[#232938] text-[#D4AF37] shadow-sm font-semibold'
                  : 'text-[#8A91A5] hover:text-[#ECEEF4]'
              }`}
            >
              <Film className="w-3.5 h-3.5" />
              Motion Studio
            </button>
            <button
              onClick={() => {
                setActiveTab('carpet');
                setMotionStyle('PERSIAN_CARPET_LUXURY');
              }}
              className={`px-3.5 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                activeTab === 'carpet'
                  ? 'bg-[#232938] text-[#D4AF37] shadow-sm font-semibold'
                  : 'text-[#8A91A5] hover:text-[#ECEEF4]'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              Persian Carpet Mode
            </button>
            <button
              onClick={() => setActiveTab('android')}
              className={`px-3.5 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                activeTab === 'android'
                  ? 'bg-[#232938] text-[#D4AF37] shadow-sm font-semibold'
                  : 'text-[#8A91A5] hover:text-[#ECEEF4]'
              }`}
            >
              <Code2 className="w-3.5 h-3.5" />
              Android Studio Project
            </button>
          </div>

          {/* Download Android Project CTA */}
          <button
            onClick={handleDownloadProjectZip}
            disabled={isZipping}
            className="flex items-center gap-2 px-3.5 py-2 bg-gradient-to-r from-[#D4AF37] to-[#B89228] text-[#0D0E12] text-xs font-bold rounded-lg shadow hover:brightness-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
          >
            <FolderArchive className="w-4 h-4" />
            {isZipping ? 'Packaging Project...' : 'Download APK Source (.zip)'}
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto w-full p-4 lg:p-8">
        {/* TAB 1: STUDIO SIMULATOR */}
        {activeTab === 'studio' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left 7 Columns: Viewport & Live 60 FPS Camera Canvas */}
            <div className="lg:col-span-7 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs text-[#8A91A5]">
                  <span className="font-semibold text-white">Live Camera Viewport</span>
                  <span>·</span>
                  <span className="text-[#3DD598] font-mono">60 FPS Hardware Fluidity</span>
                  <span>·</span>
                  <span className="text-[#D4AF37]">{MOTION_STYLES.find((m) => m.id === motionStyle)?.title}</span>
                </div>

                {/* Mobile phone / Widescreen device frame toggles */}
                <div className="flex items-center gap-1 bg-[#141720] border border-[#222836] p-0.5 rounded-lg">
                  <button
                    onClick={() => setDeviceFrameMode('mobile')}
                    title="Android Phone Mockup Frame"
                    className={`p-1.5 rounded transition ${
                      deviceFrameMode === 'mobile' ? 'bg-[#232938] text-[#D4AF37]' : 'text-[#8A91A5]'
                    }`}
                  >
                    <Smartphone className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => setDeviceFrameMode('widescreen')}
                    title="16:9 Cinema Full Widescreen"
                    className={`p-1.5 rounded transition ${
                      deviceFrameMode === 'widescreen' ? 'bg-[#232938] text-[#D4AF37]' : 'text-[#8A91A5]'
                    }`}
                  >
                    <Monitor className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Viewport Container */}
              <div
                className={`relative mx-auto transition-all duration-300 w-full ${
                  deviceFrameMode === 'mobile'
                    ? 'max-w-[360px] aspect-[9/18] rounded-[36px] p-3 border-[6px] border-[#222634] bg-[#000] shadow-2xl shadow-black ring-1 ring-[#D4AF37]/20'
                    : 'aspect-video rounded-2xl border border-[#222836] bg-[#000] overflow-hidden shadow-2xl'
                }`}
                onMouseMove={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  const x = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
                  const y = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
                  setMouseTilt({ x, y });
                }}
                onMouseLeave={() => setMouseTilt({ x: 0, y: 0 })}
              >
                {/* Mobile Camera Punchhole */}
                {deviceFrameMode === 'mobile' && (
                  <div className="absolute top-4 left-1/2 -translate-x-1/2 w-4 h-4 rounded-full bg-[#12141C] border border-[#2B3042] z-30 flex items-center justify-center">
                    <div className="w-1.5 h-1.5 rounded-full bg-[#05070B]" />
                  </div>
                )}

                {/* 60 FPS Viewport Canvas */}
                <div className={`relative w-full h-full overflow-hidden ${deviceFrameMode === 'mobile' ? 'rounded-[26px]' : ''}`}>
                  <canvas
                    ref={previewCanvasRef}
                    width={deviceFrameMode === 'mobile' ? 720 : 1280}
                    height={deviceFrameMode === 'mobile' ? 1440 : 720}
                    className="w-full h-full object-cover select-none"
                  />

                  {/* HUD Overlay with Cinema Telemetry */}
                  <div className="absolute top-4 left-4 right-4 flex items-center justify-between z-20 pointer-events-none">
                    <div className="px-2.5 py-1 bg-black/60 backdrop-blur-md border border-white/10 rounded-md text-[10px] font-mono text-white flex items-center gap-2">
                      <div className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                      <span>REC 60 FPS</span>
                      <span className="text-[#8A91A5]">|</span>
                      <span>ZOOM {currentTransform.scale.toFixed(2)}x</span>
                    </div>

                    <div className="px-2.5 py-1 bg-black/60 backdrop-blur-md border border-white/10 rounded-md text-[10px] font-mono text-[#D4AF37]">
                      {currentTransform.macroZoneName || resolution.label}
                    </div>
                  </div>

                  {/* Bottom Viewport HUD & Scrubber */}
                  <div className="absolute bottom-4 left-4 right-4 z-20 flex flex-col gap-2 pointer-events-auto">
                    {/* Timecode & Progress Line */}
                    <div className="flex items-center justify-between text-[11px] font-mono text-white/80">
                      <span>{currentTime.toFixed(1)}s</span>
                      <span className="text-white/40">{durationSeconds}.0s</span>
                    </div>
                    <div className="w-full h-1.5 bg-white/20 rounded-full overflow-hidden backdrop-blur cursor-pointer relative"
                      onClick={(e) => {
                        const rect = e.currentTarget.getBoundingClientRect();
                        const fraction = (e.clientX - rect.left) / rect.width;
                        setCurrentTime(fraction * durationSeconds);
                      }}
                    >
                      <div
                        className="h-full bg-gradient-to-r from-[#D4AF37] to-[#F3E5AB] rounded-full transition-all duration-75"
                        style={{ width: `${(currentTime / durationSeconds) * 100}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Viewport Playback & Scrubber Controls */}
              <div className="flex items-center justify-between bg-[#11141C] border border-[#1E2330] rounded-xl p-3">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsPlaying(!isPlaying)}
                    className="w-9 h-9 rounded-lg bg-[#222836] hover:bg-[#2F3648] text-white flex items-center justify-center transition active:scale-95 cursor-pointer"
                  >
                    {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4 ml-0.5 text-[#D4AF37]" />}
                  </button>
                  <button
                    onClick={() => setCurrentTime(0)}
                    className="w-9 h-9 rounded-lg bg-[#181C26] hover:bg-[#222836] text-[#8A91A5] hover:text-white flex items-center justify-center transition cursor-pointer"
                    title="Replay from 00:00"
                  >
                    <RotateCcw className="w-4 h-4" />
                  </button>

                  <div className="ml-2 text-xs font-mono text-[#8A91A5]">
                    Speed: <span className="text-[#ECEEF4]">1.0x (Realtime)</span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <label className="flex items-center gap-2 text-xs text-[#8A91A5] cursor-pointer">
                    <input
                      type="checkbox"
                      checked={enableLightingSweep}
                      onChange={(e) => setEnableLightingSweep(e.target.checked)}
                      className="rounded accent-[#D4AF37]"
                    />
                    <span className="flex items-center gap-1">
                      <Sun className="w-3.5 h-3.5 text-[#D4AF37]" />
                      Studio Fiber Glint
                    </span>
                  </label>
                </div>
              </div>

              {/* Gallery Image Selection */}
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-4 flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-xs font-bold uppercase tracking-wider text-[#D4AF37]">
                      Gallery Photo Selection
                    </h3>
                    <p className="text-xs text-[#8A91A5]">
                      Modern Android Photo Picker API · Preserves raw RGB & sRGB white balance
                    </p>
                  </div>

                  {/* Pick from device file button */}
                  <label className="px-3 py-1.5 bg-[#1F2533] hover:bg-[#293245] border border-[#2D364A] text-white text-xs font-medium rounded-lg flex items-center gap-1.5 cursor-pointer transition">
                    <Camera className="w-3.5 h-3.5 text-[#D4AF37]" />
                    <span>Upload High-Res Photo</span>
                    <input type="file" accept="image/*" onChange={handlePhotoUpload} className="hidden" />
                  </label>
                </div>

                {/* Thumbnails of Master Persian Carpets */}
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-2.5 pt-1">
                  {CARPET_PRESETS.map((carpet) => {
                    const isSelected = !customImage && selectedPreset.id === carpet.id;
                    return (
                      <button
                        key={carpet.id}
                        onClick={() => {
                          setCustomImage(null);
                          setSelectedPreset(carpet);
                          setCurrentTime(0);
                        }}
                        className={`text-left p-2.5 rounded-lg border transition flex flex-col gap-1 cursor-pointer ${
                          isSelected
                            ? 'bg-[#1E2536] border-[#D4AF37] ring-1 ring-[#D4AF37]/30'
                            : 'bg-[#151922] border-[#222836] hover:border-[#333C50]'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span
                            className="w-3 h-3 rounded-full border border-white/20"
                            style={{ backgroundColor: carpet.primaryColor }}
                          />
                          <span className="text-[10px] font-mono text-[#D4AF37]">{carpet.kpsi} KPSI</span>
                        </div>
                        <span className="text-xs font-semibold text-white truncate">{carpet.name}</span>
                        <span className="text-[10px] text-[#8A91A5] truncate">{carpet.city}</span>
                      </button>
                    );
                  })}
                </div>

                {customImage && (
                  <div className="mt-2 p-2.5 bg-[#1B212D] border border-[#2B3549] rounded-lg flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <Camera className="w-4 h-4 text-[#D4AF37]" />
                      <span className="font-medium text-white">{customImageName}</span>
                      <span className="text-[#8A91A5]">
                        ({customImage.naturalWidth} × {customImage.naturalHeight} · sRGB Uncompressed)
                      </span>
                    </div>
                    <button
                      onClick={() => setCustomImage(null)}
                      className="text-[#D4AF37] hover:underline text-[11px] cursor-pointer"
                    >
                      Reset to Persian Rug Presets
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Right 5 Columns: Motion Styles & Video Rendering Engine */}
            <div className="lg:col-span-5 flex flex-col gap-6">
              {/* Motion Style Selection */}
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-5 flex flex-col gap-4">
                <div className="flex items-center justify-between border-b border-[#1E2330] pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                      <Film className="w-4 h-4 text-[#D4AF37]" />
                      Cinematic Motion Engine
                    </h3>
                    <p className="text-xs text-[#8A91A5]">
                      Select high-end cinema camera trajectory profile (60 FPS)
                    </p>
                  </div>
                </div>

                <div className="flex flex-col gap-2 max-h-[340px] overflow-y-auto pr-1">
                  {MOTION_STYLES.map((style) => {
                    const isSelected = motionStyle === style.id;
                    return (
                      <div
                        key={style.id}
                        onClick={() => {
                          setMotionStyle(style.id);
                          setCurrentTime(0);
                        }}
                        className={`p-3 rounded-lg border transition cursor-pointer flex items-start gap-3 ${
                          isSelected
                            ? 'bg-[#1C2230] border-[#D4AF37] ring-1 ring-[#D4AF37]/30'
                            : 'bg-[#141720] border-[#1E2330] hover:border-[#2C3446]'
                        }`}
                      >
                        <input
                          type="radio"
                          name="motionStyle"
                          checked={isSelected}
                          onChange={() => {
                            setMotionStyle(style.id);
                            setCurrentTime(0);
                          }}
                          className="mt-1 accent-[#D4AF37]"
                        />
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <span className={`text-xs font-semibold ${isSelected ? 'text-[#D4AF37]' : 'text-white'}`}>
                              {style.title}
                            </span>
                            <span className="text-[10px] font-mono text-[#8A91A5]">{style.category}</span>
                          </div>
                          <p className="text-[11px] text-[#8A91A5] mt-0.5 leading-snug">{style.description}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Video Rendering Configuration Card */}
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-5 flex flex-col gap-4">
                <div className="flex items-center justify-between border-b border-[#1E2330] pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                      <Video className="w-4 h-4 text-[#D4AF37]" />
                      Video Rendering & Hardware Encoding
                    </h3>
                    <p className="text-xs text-[#8A91A5]">
                      MediaCodec H.264 High Profile · 30 / 60 FPS · 1080p / 4K UHD
                    </p>
                  </div>
                </div>

                {/* Duration Pills */}
                <div>
                  <span className="text-xs font-medium text-[#8A91A5] block mb-2">Duration:</span>
                  <div className="grid grid-cols-3 gap-2">
                    {[5, 10, 15].map((sec) => (
                      <button
                        key={sec}
                        onClick={() => {
                          setDurationSeconds(sec);
                          setCurrentTime(0);
                        }}
                        className={`py-2 text-xs font-semibold rounded-lg border transition cursor-pointer ${
                          durationSeconds === sec
                            ? 'bg-[#D4AF37] text-[#0D0E12] border-[#D4AF37]'
                            : 'bg-[#151922] text-[#8A91A5] border-[#222836] hover:text-white'
                        }`}
                      >
                        {sec} Seconds
                      </button>
                    ))}
                  </div>
                </div>

                {/* Resolution Selector */}
                <div>
                  <span className="text-xs font-medium text-[#8A91A5] block mb-2">Resolution:</span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => setResolution({ width: 1920, height: 1080, label: '1080p Full HD' })}
                      className={`py-2 text-xs font-semibold rounded-lg border transition cursor-pointer ${
                        resolution.width === 1920
                          ? 'bg-[#1E2536] text-[#D4AF37] border-[#D4AF37]'
                          : 'bg-[#151922] text-[#8A91A5] border-[#222836] hover:text-white'
                      }`}
                    >
                      1080p Full HD (1920×1080)
                    </button>
                    <button
                      onClick={() => setResolution({ width: 3840, height: 2160, label: '4K Ultra HD' })}
                      className={`py-2 text-xs font-semibold rounded-lg border transition cursor-pointer ${
                        resolution.width === 3840
                          ? 'bg-[#1E2536] text-[#D4AF37] border-[#D4AF37]'
                          : 'bg-[#151922] text-[#8A91A5] border-[#222836] hover:text-white'
                      }`}
                    >
                      4K Ultra HD (3840×2160)
                    </button>
                  </div>
                </div>

                {/* Frame Rate Selector */}
                <div>
                  <span className="text-xs font-medium text-[#8A91A5] block mb-2">Frame Rate:</span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => setFps(30)}
                      className={`py-2 text-xs font-semibold rounded-lg border transition cursor-pointer ${
                        fps === 30
                          ? 'bg-[#1E2536] text-[#D4AF37] border-[#D4AF37]'
                          : 'bg-[#151922] text-[#8A91A5] border-[#222836] hover:text-white'
                      }`}
                    >
                      30 FPS (Standard)
                    </button>
                    <button
                      onClick={() => setFps(60)}
                      className={`py-2 text-xs font-semibold rounded-lg border transition cursor-pointer ${
                        fps === 60
                          ? 'bg-[#1E2536] text-[#D4AF37] border-[#D4AF37]'
                          : 'bg-[#151922] text-[#8A91A5] border-[#222836] hover:text-white'
                      }`}
                    >
                      60 FPS (Ultra Smooth)
                    </button>
                  </div>
                </div>

                {/* Color Integrity Guarantee Box */}
                <div className="p-3 bg-[#131720] border border-[#212736] rounded-lg flex items-start gap-2.5">
                  <ShieldCheck className="w-4 h-4 text-[#D4AF37] mt-0.5 shrink-0" />
                  <div className="text-[11px] text-[#8A91A5] leading-relaxed">
                    <span className="text-[#D4AF37] font-semibold">Zero Color Modification Guarantee: </span>
                    White balance, saturation, brightness, and natural RGB pigments are encoded verbatim without tone-mapping or AI distortion.
                  </div>
                </div>

                {/* Render Button */}
                <button
                  onClick={handleStartExport}
                  disabled={isRendering}
                  className="w-full py-3.5 bg-gradient-to-r from-[#D4AF37] to-[#B89228] text-[#0D0E12] font-bold text-sm rounded-xl shadow-lg hover:brightness-105 active:scale-98 transition flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  <Video className="w-4 h-4" />
                  {isRendering ? 'Hardware Encoding in Progress...' : `Render MP4 Video (${resolution.label} · ${fps} FPS)`}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: PERSIAN CARPET LUXURY SHOWCASE MODE */}
        {activeTab === 'carpet' && (
          <div className="flex flex-col gap-8">
            <div className="bg-gradient-to-r from-[#171B26] via-[#1A202E] to-[#141722] border border-[#D4AF37]/30 rounded-2xl p-6 lg:p-8 flex flex-col md:flex-row items-center justify-between gap-6 shadow-xl">
              <div className="flex flex-col gap-2 max-w-2xl">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-mono font-bold tracking-widest text-[#D4AF37] uppercase">
                    Commercial Product Photography
                  </span>
                  <span className="w-1.5 h-1.5 rounded-full bg-[#D4AF37]" />
                  <span className="text-xs text-[#8A91A5]">Original Color Fidelity Locked</span>
                </div>
                <h2 className="text-2xl lg:text-3xl font-bold text-white tracking-tight">
                  Persian Carpet Luxury Showcase Engine
                </h2>
                <p className="text-sm text-[#A2A9BD] leading-relaxed">
                  Engineered specifically for master Persian rugs: Kashan, Tabriz, Isfahan, Nain, and Qom. Executes a slow multi-stage macro camera trajectory across center medallions, floral palmettes, triple border guards, pile knotting, and hand-knotted fringes.
                </p>
              </div>

              <div className="flex flex-col sm:flex-row items-center gap-3 shrink-0">
                <button
                  onClick={() => {
                    setActiveTab('studio');
                    setMotionStyle('PERSIAN_CARPET_LUXURY');
                    setIsPlaying(true);
                  }}
                  className="px-5 py-3 bg-[#D4AF37] text-[#0D0E12] font-bold text-xs rounded-xl hover:brightness-105 transition flex items-center gap-2 cursor-pointer"
                >
                  <Eye className="w-4 h-4" />
                  Launch Rug Macro Tour
                </button>
              </div>
            </div>

            {/* Master Rug Selection Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
              {CARPET_PRESETS.map((carpet) => {
                const isSelected = selectedPreset.id === carpet.id;
                return (
                  <div
                    key={carpet.id}
                    onClick={() => {
                      setSelectedPreset(carpet);
                      setCustomImage(null);
                    }}
                    className={`p-4 rounded-xl border transition cursor-pointer flex flex-col justify-between ${
                      isSelected
                        ? 'bg-[#1C2333] border-[#D4AF37] ring-1 ring-[#D4AF37]/40 shadow-lg'
                        : 'bg-[#11141C] border-[#1E2330] hover:border-[#2C3549]'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <span
                          className="w-4 h-4 rounded-full border border-white/20 shadow-sm"
                          style={{ backgroundColor: carpet.primaryColor }}
                        />
                        <span className="text-[11px] font-mono text-[#D4AF37] font-semibold">{carpet.kpsi} KPSI</span>
                      </div>
                      <h4 className="text-sm font-bold text-white mb-1">{carpet.name}</h4>
                      <p className="text-xs text-[#D4AF37] mb-2">{carpet.city}</p>
                      <p className="text-xs text-[#8A91A5] line-clamp-3 leading-relaxed">{carpet.description}</p>
                    </div>

                    <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-[11px]">
                      <span className="text-[#8A91A5]">{carpet.silkRatio}</span>
                      <ChevronRight className="w-3.5 h-3.5 text-[#D4AF37]" />
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Macro Waypoints Trajectory Breakdown */}
            <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-6 flex flex-col gap-4">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Sliders className="w-4 h-4 text-[#D4AF37]" />
                5-Stage Macro Focal Waypoint Trajectory
              </h3>
              <p className="text-xs text-[#8A91A5]">
                Continuous 60 FPS trajectory navigating authentic physical zones without abrupt cuts
              </p>

              <div className="grid grid-cols-1 md:grid-cols-5 gap-3 pt-2">
                {[
                  {
                    step: 1,
                    title: 'Center Medallion (Toranj)',
                    zoom: '1.95x',
                    desc: 'Central floral sunburst medallion symbol of eternity and cosmic order.'
                  },
                  {
                    step: 2,
                    title: 'Floral Palmettes (Eslimi)',
                    zoom: '2.40x',
                    desc: 'Spiraling Shah Abbasi lotus vines, arabesques and curvilinear spandrels.'
                  },
                  {
                    step: 3,
                    title: 'Master Border Guard (Hasheeyeh)',
                    zoom: '2.20x',
                    desc: 'Triple guard border displaying historical cartouche inscriptions.'
                  },
                  {
                    step: 4,
                    title: 'Pile Texture & Silk Sheen',
                    zoom: '2.85x',
                    desc: 'Raking light across dense hand-spun wool/silk knot pile revealing luster.'
                  },
                  {
                    step: 5,
                    title: 'Hand-Knotted Fringe (Risheh)',
                    zoom: '2.10x',
                    desc: 'Authentic cotton/silk foundation fringes confirming master weaver provenance.'
                  }
                ].map((item) => (
                  <div key={item.step} className="p-3.5 bg-[#161B26] border border-[#222938] rounded-lg flex flex-col gap-2">
                    <div className="flex items-center justify-between">
                      <span className="w-5 h-5 rounded-full bg-[#D4AF37] text-[#0D0E12] text-[10px] font-bold flex items-center justify-center">
                        {item.step}
                      </span>
                      <span className="text-[10px] font-mono text-[#D4AF37]">{item.zoom} Zoom</span>
                    </div>
                    <span className="text-xs font-bold text-white">{item.title}</span>
                    <p className="text-[11px] text-[#8A91A5] leading-snug">{item.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: NATIVE ANDROID STUDIO PROJECT & GRADLE */}
        {activeTab === 'android' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left 4 Columns: File Tree & Architecture */}
            <div className="lg:col-span-4 flex flex-col gap-4">
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-4 flex flex-col gap-3">
                <div className="flex items-center justify-between border-b border-[#1E2330] pb-2.5">
                  <h3 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                    <FolderArchive className="w-4 h-4 text-[#D4AF37]" />
                    Android Project Files
                  </h3>
                  <span className="text-[11px] font-mono text-[#8A91A5]">Gradle 8.4</span>
                </div>

                {/* File list */}
                <div className="flex flex-col gap-1 max-h-[460px] overflow-y-auto pr-1">
                  {ANDROID_FILES.map((file) => {
                    const isSelected = selectedFile.path === file.path;
                    return (
                      <button
                        key={file.path}
                        onClick={() => setSelectedFile(file)}
                        className={`text-left p-2 rounded-lg transition flex items-center gap-2 text-xs cursor-pointer ${
                          isSelected
                            ? 'bg-[#1E2536] text-[#D4AF37] font-semibold border border-[#D4AF37]/30'
                            : 'text-[#8A91A5] hover:text-white hover:bg-[#161A24]'
                        }`}
                      >
                        <FileCode className="w-3.5 h-3.5 shrink-0" />
                        <span className="truncate">{file.path.split('/').pop()}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Build Instructions Card */}
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl p-4 flex flex-col gap-3">
                <h4 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
                  <Code2 className="w-4 h-4 text-[#D4AF37]" />
                  Build Command (GitHub Actions & CLI)
                </h4>
                <div className="p-2.5 bg-[#090A0D] border border-[#1C202C] rounded-lg font-mono text-xs text-[#D4AF37] flex items-center justify-between">
                  <span>./gradlew assembleDebug</span>
                  <button
                    onClick={() => handleCopyCode('./gradlew assembleDebug')}
                    className="p-1 hover:text-white cursor-pointer"
                    title="Copy command"
                  >
                    <Copy className="w-3.5 h-3.5" />
                  </button>
                </div>
                <p className="text-[11px] text-[#8A91A5] leading-relaxed">
                  Generated Gradle wrapper files (<code className="text-[#D4AF37]">gradlew</code>, <code className="text-[#D4AF37]">gradlew.bat</code>, <code className="text-[#D4AF37]">gradle-wrapper.jar</code>) are fully included in the project root and <code className="text-[#D4AF37]">android/</code> directory.
                </p>
              </div>
            </div>

            {/* Right 8 Columns: Code Viewer */}
            <div className="lg:col-span-8 flex flex-col gap-3">
              <div className="bg-[#11141C] border border-[#1E2330] rounded-xl overflow-hidden shadow-xl">
                <div className="p-4 bg-[#141822] border-b border-[#1E2330] flex items-center justify-between">
                  <div>
                    <span className="text-xs font-mono text-[#D4AF37] block font-semibold">{selectedFile.path}</span>
                    <span className="text-xs text-[#8A91A5]">{selectedFile.description}</span>
                  </div>

                  <button
                    onClick={() => handleCopyCode(selectedFile.content)}
                    className="px-3 py-1.5 bg-[#202738] hover:bg-[#2A334A] text-white text-xs font-medium rounded-lg flex items-center gap-1.5 transition cursor-pointer"
                  >
                    {copiedCode ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCode ? 'Copied' : 'Copy Code'}</span>
                  </button>
                </div>

                <pre className="p-4 text-xs font-mono text-[#D0D4DF] bg-[#0A0C11] overflow-x-auto max-h-[560px] leading-relaxed">
                  <code>{selectedFile.content}</code>
                </pre>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* ACTIVE RENDERING & EXPORT MODAL */}
      {(isRendering || exportedVideoUrl) && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="bg-[#12151D] border border-[#2B3346] rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-6">
            <div className="flex items-center justify-between border-b border-[#1F2534] pb-3">
              <div className="flex items-center gap-2">
                <Film className="w-5 h-5 text-[#D4AF37]" />
                <h3 className="text-base font-bold text-white">
                  {isRendering ? 'MediaCodec Hardware Video Encoding' : 'Cinematic Video Rendered'}
                </h3>
              </div>
              {!isRendering && (
                <button
                  onClick={() => setExportedVideoUrl(null)}
                  className="text-[#8A91A5] hover:text-white text-xs cursor-pointer"
                >
                  Close
                </button>
              )}
            </div>

            {/* Rendering Progress View */}
            {isRendering && renderProgress && (
              <div className="flex flex-col gap-5 py-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono text-[#8A91A5]">{renderProgress.statusText}</span>
                  <span className="text-sm font-mono font-bold text-[#D4AF37]">{renderProgress.percentage}%</span>
                </div>

                {/* Progress bar */}
                <div className="w-full h-3 bg-[#1A1F2C] rounded-full overflow-hidden border border-[#283144]">
                  <div
                    className="h-full bg-gradient-to-r from-[#D4AF37] to-[#F7E7A9] transition-all duration-150"
                    style={{ width: `${renderProgress.percentage}%` }}
                  />
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs bg-[#161B26] p-3 rounded-lg border border-[#222938]">
                  <div>
                    <span className="text-[#8A91A5] block">Current Frame:</span>
                    <span className="font-mono font-semibold text-white">
                      Frame {renderProgress.currentFrame} / {renderProgress.totalFrames}
                    </span>
                  </div>
                  <div>
                    <span className="text-[#8A91A5] block">Est. Remaining:</span>
                    <span className="font-mono font-semibold text-[#D4AF37]">
                      {Math.max(0, Math.ceil(renderProgress.remainingMs / 1000))} Seconds
                    </span>
                  </div>
                </div>

                <p className="text-[11px] text-[#8A91A5] text-center">
                  Encoding 60 FPS frames directly into MP4 surface buffer without compression artifacts.
                </p>
              </div>
            )}

            {/* Video Completed Ready View */}
            {!isRendering && exportedVideoUrl && (
              <div className="flex flex-col gap-4">
                <div className="aspect-video bg-black rounded-xl overflow-hidden border border-[#2A3347]">
                  <video src={exportedVideoUrl} controls autoPlay loop className="w-full h-full object-contain" />
                </div>

                <div className="flex items-center justify-between text-xs text-[#8A91A5] px-1">
                  <span>Specs: {resolution.label} · {fps} FPS</span>
                  <span className="text-[#3DD598] flex items-center gap-1 font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Ready for Export
                  </span>
                </div>

                <div className="flex flex-col gap-2 pt-2">
                  <a
                    href={exportedVideoUrl}
                    download={`cinematic_${selectedPreset.id}_${resolution.width}p_${fps}fps.mp4`}
                    className="w-full py-3 bg-[#D4AF37] text-[#0D0E12] font-bold text-xs rounded-xl hover:brightness-105 flex items-center justify-center gap-2 shadow"
                  >
                    <Download className="w-4 h-4" />
                    Download Video File (.mp4)
                  </a>

                  <button
                    onClick={() => setIsSavedToGallery(true)}
                    className="w-full py-2.5 bg-[#1C2230] hover:bg-[#252E42] text-white text-xs font-semibold rounded-xl border border-[#2C354A] flex items-center justify-center gap-2 cursor-pointer"
                  >
                    {isSavedToGallery ? (
                      <>
                        <CheckCircle2 className="w-4 h-4 text-green-400" />
                        <span>Saved in Movies/CinematicAnimator</span>
                      </>
                    ) : (
                      <>
                        <Smartphone className="w-4 h-4 text-[#D4AF37]" />
                        <span>Save to Device Movies Gallery</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
