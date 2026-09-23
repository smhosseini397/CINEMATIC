// Procedural high-resolution Persian Rug generator for authentic previewing
// Generates detailed 2048x1365 canvas textures for Kashan, Tabriz, Isfahan, Nain, and Qom rugs

export interface CarpetPreset {
  id: string;
  name: string;
  city: string;
  kpsi: number; // Knots per square inch
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  silkRatio: string;
  description: string;
}

export const CARPET_PRESETS: CarpetPreset[] = [
  {
    id: 'kashan',
    name: 'Kashan Royal Medallion',
    city: 'Kashan, Isfahan Province',
    kpsi: 450,
    primaryColor: '#8B1E2D', // Deep Madder Ruby
    secondaryColor: '#1A2744', // Midnight Indigo
    accentColor: '#D4AF37', // Persian Gold
    silkRatio: '20% Silk Highlights / 80% Fine Kork Wool',
    description: 'Central floral sunburst medallion (Toranj) flanked by weeping willows and Shah Abbasi lotus blossoms with traditional triple border guards.'
  },
  {
    id: 'tabriz',
    name: 'Tabriz 70-Raj Masterpiece',
    city: 'Tabriz, East Azerbaijan',
    kpsi: 620,
    primaryColor: '#7A1C20', // Terracotta Wine
    secondaryColor: '#2B3A42', // Prussian Slate
    accentColor: '#E6C687', // Antique Silk Ivory
    silkRatio: '45% High-Grade Silk / 55% Fine Merino Wool',
    description: 'Extraordinary knot density featuring intricate paisley cartouches, hunting spandrels, and luminous silk outlines that capture natural raking light.'
  },
  {
    id: 'isfahan',
    name: 'Isfahan Royal Ceiling Motif',
    city: 'Isfahan, Safavid Capital',
    kpsi: 750,
    primaryColor: '#EBE3D5', // Safavid Ivory Field
    secondaryColor: '#16294D', // Royal Cobalt
    accentColor: '#C49746', // Burnished Gold
    silkRatio: 'Pure Silk Warp & Foundation / 50% Silk Pile',
    description: 'Inspired by the celestial ceiling tilework of Sheikh Lotfollah Mosque. Seamless arabesque spirals expanding outwards from a golden central vortex.'
  },
  {
    id: 'nain',
    name: 'Nain Habibian 6La',
    city: 'Nain, Central Desert',
    kpsi: 680,
    primaryColor: '#12233C', // Deep Desert Indigo
    secondaryColor: '#F5F2EB', // Bone Ivory
    accentColor: '#A8C5DA', // Celestial Blue
    silkRatio: '35% Silk Outlines / 65% Fine Lambswool',
    description: 'Renowned 6La construction with razor-sharp floral palmettes, curvilinear vines, and crisp ivory borders woven under master Habibian guidelines.'
  },
  {
    id: 'qom',
    name: 'Qom 100% Pure Mulberry Silk',
    city: 'Qom, Holy City',
    kpsi: 950,
    primaryColor: '#104E5B', // Persian Turquoise Jade
    secondaryColor: '#6B1D2F', // Pomegranate Crimson
    accentColor: '#F0E2A8', // Raw Spun Gold
    silkRatio: '100% Pure Natural Mulberry Silk',
    description: 'Ultra-luxurious pure silk pile and silk foundation. Exceptional natural luster that dynamically shifts sheen when light passes along the pile nap.'
  }
];

export function renderCarpetToCanvas(canvas: HTMLCanvasElement, preset: CarpetPreset) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2;

  // Background base
  ctx.fillStyle = preset.primaryColor;
  ctx.fillRect(0, 0, w, h);

  // Field texture simulation (wool grain)
  ctx.fillStyle = 'rgba(0,0,0,0.08)';
  for (let y = 0; y < h; y += 4) {
    ctx.fillRect(0, y, w, 1);
  }

  // Outer Border (Guard & Hasheeyeh)
  const borderThickness = Math.min(w, h) * 0.14;
  ctx.strokeStyle = preset.secondaryColor;
  ctx.lineWidth = borderThickness;
  ctx.strokeRect(borderThickness / 2, borderThickness / 2, w - borderThickness, h - borderThickness);

  // Border Gold Trim
  ctx.strokeStyle = preset.accentColor;
  ctx.lineWidth = 4;
  ctx.strokeRect(borderThickness, borderThickness, w - borderThickness * 2, h - borderThickness * 2);
  ctx.strokeRect(borderThickness * 0.25, borderThickness * 0.25, w - borderThickness * 0.5, h - borderThickness * 0.5);

  // Corner Spandrels (Lachak)
  const cornerSize = borderThickness * 2.2;
  const drawCorner = (x: number, y: number, rot: number) => {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rot);
    ctx.fillStyle = preset.secondaryColor;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(cornerSize, 0);
    ctx.bezierCurveTo(cornerSize * 0.6, cornerSize * 0.3, cornerSize * 0.3, cornerSize * 0.6, 0, cornerSize);
    ctx.closePath();
    ctx.fill();

    // Spandrel gold details
    ctx.strokeStyle = preset.accentColor;
    ctx.lineWidth = 3;
    ctx.stroke();

    // Spandrel floral spiral
    ctx.beginPath();
    ctx.arc(cornerSize * 0.3, cornerSize * 0.3, cornerSize * 0.18, 0, Math.PI * 1.5);
    ctx.stroke();

    ctx.restore();
  };

  const inset = borderThickness;
  drawCorner(inset, inset, 0);
  drawCorner(w - inset, inset, Math.PI / 2);
  drawCorner(w - inset, h - inset, Math.PI);
  drawCorner(inset, h - inset, -Math.PI / 2);

  // Central Field Inner Trim
  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.lineWidth = 2;
  ctx.strokeRect(borderThickness + 10, borderThickness + 10, w - (borderThickness + 10) * 2, h - (borderThickness + 10) * 2);

  // Intricate Central Medallion (Toranj)
  const medRadius = Math.min(w, h) * 0.24;

  // Medallion secondary base
  ctx.fillStyle = preset.secondaryColor;
  ctx.beginPath();
  ctx.ellipse(cx, cy, medRadius * 1.15, medRadius * 0.95, 0, 0, Math.PI * 2);
  ctx.fill();

  // Medallion outer starburst petals (16 lobes)
  ctx.fillStyle = preset.accentColor;
  const lobes = 16;
  ctx.beginPath();
  for (let i = 0; i < lobes; i++) {
    const angle = (i * 2 * Math.PI) / lobes;
    const rOuter = medRadius * 1.1;
    const rInner = medRadius * 0.85;
    const px1 = cx + Math.cos(angle) * rOuter;
    const py1 = cy + Math.sin(angle) * rOuter;
    const px2 = cx + Math.cos(angle + Math.PI / lobes) * rInner;
    const py2 = cy + Math.sin(angle + Math.PI / lobes) * rInner;
    if (i === 0) ctx.moveTo(px1, py1);
    else ctx.lineTo(px1, py1);
    ctx.lineTo(px2, py2);
  }
  ctx.closePath();
  ctx.fill();

  // Medallion primary core
  ctx.fillStyle = preset.primaryColor;
  ctx.beginPath();
  ctx.arc(cx, cy, medRadius * 0.65, 0, Math.PI * 2);
  ctx.fill();
  ctx.strokeStyle = preset.accentColor;
  ctx.lineWidth = 4;
  ctx.stroke();

  // Central Gold Flower Core
  ctx.fillStyle = preset.accentColor;
  ctx.beginPath();
  ctx.arc(cx, cy, medRadius * 0.25, 0, Math.PI * 2);
  ctx.fill();

  // Inner jewel rosette
  ctx.fillStyle = preset.secondaryColor;
  ctx.beginPath();
  ctx.arc(cx, cy, medRadius * 0.10, 0, Math.PI * 2);
  ctx.fill();

  // Pendant Medallion tips (Sar-Toranj) north & south
  const drawPendant = (px: number, py: number, flip: boolean) => {
    ctx.save();
    ctx.translate(px, py);
    ctx.fillStyle = preset.accentColor;
    ctx.beginPath();
    ctx.moveTo(0, flip ? 40 : -40);
    ctx.lineTo(20, 0);
    ctx.lineTo(0, flip ? -15 : 15);
    ctx.lineTo(-20, 0);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  };
  drawPendant(cx, cy - medRadius * 1.15, false);
  drawPendant(cx, cy + medRadius * 1.15, true);

  // Border palmettes pattern (Hasheeyeh repeat)
  ctx.fillStyle = preset.accentColor;
  for (let bx = borderThickness; bx < w - borderThickness; bx += 70) {
    ctx.beginPath();
    ctx.arc(bx, borderThickness / 2, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.arc(bx, h - borderThickness / 2, 7, 0, Math.PI * 2);
    ctx.fill();
  }
  for (let by = borderThickness; by < h - borderThickness; by += 70) {
    ctx.beginPath();
    ctx.arc(borderThickness / 2, by, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.arc(w - borderThickness / 2, by, 7, 0, Math.PI * 2);
    ctx.fill();
  }

  // Realistic Hand-Knotted Fringes (Risheh) top and bottom
  ctx.strokeStyle = '#F7F5EB'; // Natural cotton/silk warp thread
  ctx.lineWidth = 1.5;
  for (let fx = 0; fx < w; fx += 5) {
    // Top fringe
    ctx.beginPath();
    ctx.moveTo(fx, 0);
    ctx.lineTo(fx + (Math.sin(fx) * 3), -18);
    ctx.stroke();

    // Bottom fringe
    ctx.beginPath();
    ctx.moveTo(fx, h);
    ctx.lineTo(fx + (Math.sin(fx * 0.7) * 3), h + 18);
    ctx.stroke();
  }
}
