import { pick, rand, reduceMotion } from './html';

export type Palette = [string, string, string];

interface Star {
  x: number;
  y: number;
  z: number;
  pz: number;
}

interface Spark {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  decay: number;
  size: number;
  color: string;
}

export interface Background {
  warp(v?: number): void;
  burst(x: number, y: number, n?: number, palette?: string[]): void;
  fireworks(count?: number, spread?: number): () => void;
  setColors(c: Palette): void;
  size(): { W: number; H: number };
}

// starfield that warps between slides, plus spark bursts and fireworks
export function makeBackground(cv: HTMLCanvasElement): Background {
  const ctx = cv.getContext('2d')!;
  let W = 0;
  let H = 0;
  let warp = 0;
  let colors: string[] = ['#7c3aed', '#06b6d4', '#f43f5e'];
  const stars: Star[] = [];
  const sparks: Spark[] = [];

  const resize = () => {
    const dpr = Math.min(devicePixelRatio || 1, 2);
    W = innerWidth;
    H = innerHeight;
    cv.width = W * dpr;
    cv.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  };
  resize();
  addEventListener('resize', resize);

  const newStar = (z?: number): Star => ({ x: rand(-1, 1), y: rand(-1, 1), z: z ?? rand(0.05, 1), pz: 0 });
  for (let i = 0; i < 260; i++) stars.push(newStar());

  const frame = () => {
    ctx.clearRect(0, 0, W, H);
    const cx = W / 2;
    const cy = H / 2;
    const fov = Math.max(W, H) * 0.5;
    const speed = 0.0009 + warp * 0.03;
    warp *= 0.94;
    for (const s of stars) {
      s.pz = s.z;
      s.z -= speed;
      if (s.z <= 0.02) Object.assign(s, newStar(1), { pz: 1 });
      const x = cx + (s.x / s.z) * fov;
      const y = cy + (s.y / s.z) * fov;
      const px = cx + (s.x / s.pz) * fov;
      const py = cy + (s.y / s.pz) * fov;
      ctx.strokeStyle = `rgba(255,255,255,${Math.min(1, (1 - s.z) * 1.4)})`;
      ctx.lineWidth = (1 - s.z) * 2.2;
      ctx.beginPath();
      ctx.moveTo(px, py);
      ctx.lineTo(x + 0.1, y + 0.1);
      ctx.stroke();
    }
    ctx.globalCompositeOperation = 'lighter';
    for (let i = sparks.length - 1; i >= 0; i--) {
      const p = sparks[i];
      p.vx *= 0.985;
      p.vy = p.vy * 0.985 + 0.045;
      p.x += p.vx;
      p.y += p.vy;
      p.life -= p.decay;
      if (p.life <= 0) {
        sparks.splice(i, 1);
        continue;
      }
      ctx.globalAlpha = p.life;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size * (0.5 + p.life / 2), 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1;
    ctx.globalCompositeOperation = 'source-over';
    requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);

  const burst = (x: number, y: number, n = 90, palette = colors) => {
    if (reduceMotion()) return;
    for (let i = 0; i < n; i++) {
      const a = rand(0, Math.PI * 2);
      const v = rand(1.5, 7.5);
      sparks.push({
        x,
        y,
        vx: Math.cos(a) * v,
        vy: Math.sin(a) * v - 1.5,
        life: 1,
        decay: rand(0.008, 0.02),
        size: rand(1.4, 3.4),
        color: pick([...palette, '#ffffff']),
      });
    }
  };

  const fireworks = (count = 5, spread = 1600) => {
    const ids: Timeout[] = [];
    for (let i = 0; i < count; i++)
      ids.push(setTimeout(() => burst(rand(W * 0.15, W * 0.85), rand(H * 0.12, H * 0.5)), rand(0, spread)));
    return () => ids.forEach(clearTimeout);
  };

  return {
    warp: (v = 1) => (warp = Math.max(warp, v)),
    burst,
    fireworks,
    setColors: c => (colors = c),
    size: () => ({ W, H }),
  };
}
