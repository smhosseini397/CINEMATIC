export interface MotionTransform {
  scale: number;
  transX: number; // normalized (-1 to 1)
  transY: number;
  rotationZ: number; // degrees
  tiltX: number;
  tiltY: number;
  lightAngle: number;
  lightIntensity: number;
  macroZoneName?: string;
}

export type MotionStyleType =
  | 'SLOW_ZOOM_IN'
  | 'SLOW_ZOOM_OUT'
  | 'KEN_BURNS'
  | 'SMOOTH_PAN_HORIZONTAL'
  | 'VERTICAL_MOVEMENT'
  | 'PARALLAX_3D'
  | 'DOCUMENTARY_DRIFT'
  | 'LUXURY_SHOWCASE'
  | 'PERSIAN_CARPET_LUXURY';

export const MOTION_STYLES: {
  id: MotionStyleType;
  title: string;
  tagline: string;
  description: string;
  category: string;
}[] = [
  {
    id: 'SLOW_ZOOM_IN',
    title: 'Slow Cinematic Zoom In',
    tagline: 'Filmic Emotional Focus',
    description: 'Slow, steady 60 FPS acceleration into the heart of the photograph with zero jitter.',
    category: 'Cinematic Camera'
  },
  {
    id: 'SLOW_ZOOM_OUT',
    title: 'Slow Cinematic Zoom Out',
    tagline: 'Contextual Reveal',
    description: 'Pulls back gracefully from intricate micro-details to the grand composition.',
    category: 'Cinematic Camera'
  },
  {
    id: 'KEN_BURNS',
    title: 'Ken Burns Dynamic',
    tagline: 'Signature Documentary',
    description: 'Harmonious diagonal pan combined with gentle organic scale transition and slight tilt.',
    category: 'Documentary'
  },
  {
    id: 'SMOOTH_PAN_HORIZONTAL',
    title: 'Smooth Lateral Pan',
    tagline: 'Wide Panoramic Dolly',
    description: 'Glides horizontally across wide framing with smooth ease-in and ease-out curves.',
    category: 'Cinematic Camera'
  },
  {
    id: 'VERTICAL_MOVEMENT',
    title: 'Vertical Camera Glide',
    tagline: 'Elevating Crane Motion',
    description: 'Ascends vertically from tactile bottom fringes to top crown palmettes.',
    category: 'Cinematic Camera'
  },
  {
    id: 'PARALLAX_3D',
    title: '3D Parallax Depth',
    tagline: 'Multi-Plane Spatial Orbit',
    description: 'Multi-axis spatial depth displacement simulating an anamorphic prime lens on an orbital track.',
    category: '3D Spatial'
  },
  {
    id: 'DOCUMENTARY_DRIFT',
    title: 'Documentary Handheld Drift',
    tagline: 'Natural Organic Breathing',
    description: 'Simulates the gentle organic harmonic breathing of a master handheld cinema rig.',
    category: 'Documentary'
  },
  {
    id: 'LUXURY_SHOWCASE',
    title: 'Luxury Product Showcase',
    tagline: 'Commercial Studio Glint',
    description: 'Sweeping orbital arc with soft directional light glint traversing the surface.',
    category: 'Commercial'
  },
  {
    id: 'PERSIAN_CARPET_LUXURY',
    title: 'Persian Carpet Showcase',
    tagline: 'Master Rug Macro Tour',
    description: 'Sequenced macro focus across Central Medallion, Floral Palmettes, Borders, Pile Texture & Fringes.',
    category: 'Specialized Rug Mode'
  }
];

function cubicEaseInOut(t: number): number {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}

export function computeMotionTransform(
  style: MotionStyleType,
  progress: number,
  interactiveTilt: { x: number; y: number } = { x: 0, y: 0 }
): MotionTransform {
  const p = Math.max(0, Math.min(1, progress));
  const eased = cubicEaseInOut(p);

  switch (style) {
    case 'SLOW_ZOOM_IN':
      return {
        scale: 1.0 + 0.35 * eased,
        transX: 0,
        transY: 0,
        rotationZ: 0,
        tiltX: interactiveTilt.y * 2,
        tiltY: interactiveTilt.x * 2,
        lightAngle: 0,
        lightIntensity: 0
      };

    case 'SLOW_ZOOM_OUT':
      return {
        scale: 1.4 - 0.35 * eased,
        transX: 0,
        transY: 0,
        rotationZ: 0,
        tiltX: interactiveTilt.y * 2,
        tiltY: interactiveTilt.x * 2,
        lightAngle: 0,
        lightIntensity: 0
      };

    case 'KEN_BURNS': {
      const scale = 1.15 + 0.25 * eased;
      const transX = -0.12 + 0.22 * eased;
      const transY = -0.08 + 0.14 * eased;
      const rotationZ = Math.sin(eased * Math.PI) * 0.4;
      return {
        scale,
        transX,
        transY,
        rotationZ,
        tiltX: interactiveTilt.y * 3,
        tiltY: interactiveTilt.x * 3,
        lightAngle: 0,
        lightIntensity: 0
      };
    }

    case 'SMOOTH_PAN_HORIZONTAL':
      return {
        scale: 1.25,
        transX: -0.22 + 0.44 * eased,
        transY: 0,
        rotationZ: 0,
        tiltX: 0,
        tiltY: 0,
        lightAngle: 0,
        lightIntensity: 0
      };

    case 'VERTICAL_MOVEMENT':
      return {
        scale: 1.25,
        transX: 0,
        transY: 0.2 - 0.4 * eased,
        rotationZ: 0,
        tiltX: Math.sin(eased * Math.PI) * 2.5 + interactiveTilt.y * 3,
        tiltY: interactiveTilt.x * 3,
        lightAngle: 0,
        lightIntensity: 0
      };

    case 'PARALLAX_3D': {
      const angle = eased * 2 * Math.PI;
      const scale = 1.28 + 0.1 * Math.sin(eased * Math.PI);
      const transX = Math.cos(angle) * 0.08;
      const transY = Math.sin(angle) * 0.05;
      const tiltX = Math.sin(angle) * 5 + interactiveTilt.y * 6;
      const tiltY = Math.cos(angle) * 5 + interactiveTilt.x * 6;
      return {
        scale,
        transX,
        transY,
        rotationZ: 0,
        tiltX,
        tiltY,
        lightAngle: p * 360,
        lightIntensity: 0.18
      };
    }

    case 'DOCUMENTARY_DRIFT': {
      const t = eased * 4 * Math.PI;
      const transX = Math.sin(t) * 0.035 + Math.cos(t * 0.5) * 0.02;
      const transY = Math.cos(t * 0.8) * 0.03 + Math.sin(t * 0.3) * 0.015;
      const scale = 1.18 + Math.sin(t * 0.5) * 0.04;
      const rotationZ = Math.sin(t * 0.6) * 0.75;
      return {
        scale,
        transX,
        transY,
        rotationZ,
        tiltX: interactiveTilt.y * 3,
        tiltY: interactiveTilt.x * 3,
        lightAngle: 0,
        lightIntensity: 0
      };
    }

    case 'LUXURY_SHOWCASE': {
      const scale = 1.3 + Math.sin(eased * Math.PI) * 0.15;
      const transX = Math.sin((eased - 0.5) * Math.PI) * 0.18;
      const transY = Math.cos((eased - 0.5) * Math.PI) * 0.08 - 0.05;
      const lightAngle = eased * 180;
      const lightIntensity = Math.sin(eased * Math.PI) * 0.28;
      return {
        scale,
        transX,
        transY,
        rotationZ: 0,
        tiltX: Math.sin(eased * Math.PI) * 3 + interactiveTilt.y * 4,
        tiltY: Math.cos(eased * Math.PI) * 4 + interactiveTilt.x * 4,
        lightAngle,
        lightIntensity
      };
    }

    case 'PERSIAN_CARPET_LUXURY': {
      const zones = [
        { name: 'Center Medallion (Toranj)', x: 0.5, y: 0.5, zoom: 1.95 },
        { name: 'Floral Palmettes (Eslimi)', x: 0.35, y: 0.38, zoom: 2.4 },
        { name: 'Master Border (Hasheeyeh)', x: 0.18, y: 0.82, zoom: 2.2 },
        { name: 'Pile Texture & Weave', x: 0.62, y: 0.65, zoom: 2.85 },
        { name: 'Hand-Knotted Fringe (Risheh)', x: 0.5, y: 0.95, zoom: 2.1 }
      ];

      const totalStages = zones.length;
      const scaledP = p * (totalStages - 1);
      const stageIdx = Math.min(totalStages - 2, Math.floor(scaledP));
      const stageFraction = scaledP - stageIdx;
      const smoothT = stageFraction * stageFraction * (3 - 2 * stageFraction);

      const zA = zones[stageIdx];
      const zB = zones[stageIdx + 1];

      const targetX = zA.x + (zB.x - zA.x) * smoothT;
      const targetY = zA.y + (zB.y - zA.y) * smoothT;
      const scale = zA.zoom + (zB.zoom - zA.zoom) * smoothT;

      const transX = (0.5 - targetX) * (scale - 1);
      const transY = (0.5 - targetY) * (scale - 1);

      const lightAngle = p * 180;
      const lightIntensity = Math.max(0, Math.sin(p * Math.PI) * 0.12);

      return {
        scale,
        transX,
        transY,
        rotationZ: Math.sin(p * Math.PI) * 0.25,
        tiltX: Math.sin(p * 2 * Math.PI) * 2 + interactiveTilt.y * 3,
        tiltY: Math.cos(p * 2 * Math.PI) * 2 + interactiveTilt.x * 3,
        lightAngle,
        lightIntensity,
        macroZoneName: stageFraction < 0.5 ? zA.name : zB.name
      };
    }
  }
}
