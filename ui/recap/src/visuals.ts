import { easeOutExpo, esc, fmt, gauss, pick, pieceSrc, rand } from './html';
import type { Scope } from './scope';

const setupCanvas = (cv: HTMLCanvasElement, maxDpr = 2) => {
  const ctx = cv.getContext('2d')!;
  const dpr = Math.min(devicePixelRatio || 1, maxDpr);
  const W = cv.clientWidth;
  const H = cv.clientHeight;
  cv.width = W * dpr;
  cv.height = H * dpr;
  ctx.scale(dpr, dpr);
  return { ctx, W, H, dpr };
};

interface Falling {
  x: number;
  y: number;
  vy: number;
  rot: number;
  vr: number;
  img: HTMLImageElement;
  col: number;
}

// pieces with their shadow drawn once, since canvas shadows are slow to draw on every frame
const shadowPad = 12;
const sprites = new Map<HTMLImageElement, HTMLCanvasElement>();
const sprite = (img: HTMLImageElement, size: number, dpr: number) => {
  let cv = sprites.get(img);
  if (!cv) {
    cv = document.createElement('canvas');
    cv.width = cv.height = (size + shadowPad * 2) * dpr;
    const ctx = cv.getContext('2d')!;
    ctx.scale(dpr, dpr);
    ctx.shadowColor = 'rgba(0,0,0,.5)';
    ctx.shadowBlur = 8;
    ctx.drawImage(img, shadowPad, shadowPad, size, size);
    sprites.set(img, cv);
  }
  return cv;
};

const drawPiece = (ctx: CanvasRenderingContext2D, p: Falling, size: number, dpr: number) => {
  if (!p.img.complete || !p.img.naturalWidth) return;
  const half = size / 2 + shadowPad;
  ctx.save();
  ctx.translate(p.x, p.y);
  ctx.rotate(p.rot);
  ctx.drawImage(sprite(p.img, size, dpr), -half, -half, half * 2, half * 2);
  ctx.restore();
};

// pieces raining down and piling into a mound at the bottom of the screen
export function pilePieces(sc: Scope, cv: HTMLCanvasElement, count: number): void {
  const { ctx, W, H, dpr } = setupCanvas(cv);
  // pieces that landed are drawn once onto this canvas instead of every frame
  const settled = document.createElement('canvas');
  settled.width = cv.width;
  settled.height = cv.height;
  const sctx = settled.getContext('2d')!;
  sctx.scale(dpr, dpr);

  sprites.clear();
  const roles = 'PPPPNBRQK';
  const imgs: Record<string, HTMLImageElement> = {};
  for (const c of ['w', 'b'] as const)
    for (const r of 'PNBRQK') {
      const img = new Image();
      img.src = pieceSrc(c, r);
      imgs[c + r] = img;
    }
  const size = Math.max(26, Math.min(48, W / 26));
  const colW = size * 0.55;
  const cols: number[] = Array.from({ length: Math.ceil(W / colW) + 1 }, () => 0);
  const maxH = H * 0.26;
  const falling: Falling[] = [];
  let spawned = 0;

  sc.loop(t => {
    const want = Math.min(count, Math.floor((t / 4200) * count));
    while (spawned < want) {
      spawned++;
      const x = W / 2 + gauss() * W * 0.6;
      const col = Math.max(0, Math.min(cols.length - 1, Math.floor(x / colW)));
      if (cols[col] > maxH) continue;
      falling.push({
        x,
        y: -size - rand(0, 200),
        vy: rand(2, 6),
        rot: rand(-Math.PI, Math.PI),
        vr: rand(-0.15, 0.15),
        img: imgs[pick('wb') + pick(roles)],
        col,
      });
    }
    ctx.clearRect(0, 0, W, H);
    for (let i = falling.length - 1; i >= 0; i--) {
      const p = falling[i];
      p.vy += 0.55;
      p.y += p.vy;
      p.rot += p.vr;
      const floor = H - cols[p.col] - size * 0.35;
      if (p.y >= floor) {
        p.y = floor;
        drawPiece(sctx, p, size, dpr);
        const c = p.col;
        cols[c] = Math.min(maxH, cols[c] + size * 0.42);
        // let the pile slope down to its neighbours
        for (let k = 1; k < 5; k++) {
          const lim = cols[c] - k * size * 0.32;
          if (c - k >= 0) cols[c - k] = Math.min(maxH, Math.max(cols[c - k], lim));
          if (c + k < cols.length) cols[c + k] = Math.min(maxH, Math.max(cols[c + k], lim));
        }
        falling.splice(i, 1);
      } else drawPiece(ctx, p, size, dpr);
    }
    ctx.drawImage(settled, 0, 0, W, H);
    return spawned < count || falling.length > 0;
  });
}

// spiral galaxy of games with one glowing dot for the player
export function galaxy(sc: Scope, cv: HTMLCanvasElement, youLabel: string): void {
  // the dots are tiny and blurry anyway: skip high-dpi, and draw fewer on phone-sized screens
  const { ctx, W, H } = setupCanvas(cv, 1);
  const R = Math.max(W, H) * 0.55;
  const nbDots = Math.round(4000 * Math.min(1, Math.max(0.5, (W * H) / (1280 * 800))));
  // points share a few colours, so each frame sets the colour ~50 times rather than once per dot
  const groups = new Map<string, { d: number; a: number; s: number }[]>();
  for (let i = 0; i < nbDots; i++) {
    const arm = Math.floor(Math.random() * 3);
    const d = Math.pow(Math.random(), 1.6);
    const hue = Math.round(rand(200, 320) / 20) * 20;
    const shade = Math.round(d * 6) / 6;
    const style = `hsla(${hue},95%,${75 - shade * 20}%,${0.95 - shade * 0.4})`;
    if (!groups.has(style)) groups.set(style, []);
    groups.get(style)!.push({
      d,
      a: arm * ((Math.PI * 2) / 3) + d * 5.2 + gauss() * 0.45 * (1 - d * 0.5),
      s: rand(0.8, 2.6),
    });
  }
  const you = { d: 0.38, a: 2.1 };
  sc.loop(t => {
    ctx.clearRect(0, 0, W, H);
    const zoom = 0.25 + easeOutExpo(Math.min(1, t / 3000)) * 0.75;
    const rot = t / 9000;
    const cx = W / 2;
    const cy = H * 0.52;
    ctx.globalCompositeOperation = 'lighter';
    for (const [style, pts] of groups) {
      ctx.fillStyle = style;
      for (const p of pts) {
        const a = p.a + rot * (1.4 - p.d);
        ctx.fillRect(cx + Math.cos(a) * p.d * R * zoom, cy + Math.sin(a) * p.d * R * zoom * 0.55, p.s, p.s);
      }
    }
    const core = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * zoom * 0.25);
    core.addColorStop(0, 'rgba(255,240,255,.8)');
    core.addColorStop(1, 'rgba(255,240,255,0)');
    ctx.fillStyle = core;
    ctx.fillRect(0, 0, W, H);
    ctx.globalCompositeOperation = 'source-over';
    if (t > 2600) {
      const a = you.a + rot * (1.4 - you.d);
      const x = cx + Math.cos(a) * you.d * R * zoom;
      const y = cy + Math.sin(a) * you.d * R * zoom * 0.55;
      const pulse = 6 + Math.sin(t / 220) * 3;
      const g = ctx.createRadialGradient(x, y, 0, x, y, pulse * 4);
      g.addColorStop(0, 'rgba(255,255,255,1)');
      g.addColorStop(0.25, 'rgba(56,189,248,.8)');
      g.addColorStop(1, 'rgba(56,189,248,0)');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(x, y, pulse * 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.font = '800 14px system-ui, sans-serif';
      ctx.fillStyle = '#fff';
      ctx.fillText(youLabel, x + 14, y - 12);
    }
  });
}

// rough circle packing: biggest in the middle, others spiral outwards
export function layoutBubbles(box: HTMLElement, entries: [string, number][]): void {
  const max = entries[0][1];
  const palette = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#ec4899', '#14b8a6', '#6366f1'];
  const circles: { label: string; count: number; r: number; x: number; y: number }[] = [];
  for (const [label, count] of entries) {
    const r = Math.max(0.34, Math.sqrt(count / max));
    let x = 0;
    let y = 0;
    if (circles.length)
      search: for (let d = 0.2; d < 20; d += 0.04)
        for (let a = 0; a < Math.PI * 2; a += 0.15) {
          x = Math.cos(a + d) * d * 1.3;
          y = Math.sin(a + d) * d * 0.9;
          if (circles.every(o => Math.hypot(o.x - x, o.y - y) > o.r + r + 0.05)) break search;
        }
    circles.push({ label, count, r, x, y });
  }
  const minX = Math.min(...circles.map(c => c.x - c.r));
  const maxX = Math.max(...circles.map(c => c.x + c.r));
  const minY = Math.min(...circles.map(c => c.y - c.r));
  const maxY = Math.max(...circles.map(c => c.y + c.r));
  const bw = box.clientWidth;
  const bh = box.clientHeight;
  const scale = Math.min(bw / (maxX - minX), bh / (maxY - minY));
  const ox = (bw - (maxX - minX) * scale) / 2;
  const oy = (bh - (maxY - minY) * scale) / 2;
  box.innerHTML = circles
    .map((c, i) => {
      const d = c.r * 2 * scale;
      const left = ox + (c.x - c.r - minX) * scale;
      const top = oy + (c.y - c.r - minY) * scale;
      return `<div class="bubble" title="${esc(c.label)}" style="--bc:${palette[i % palette.length]};--d:${0.3 + i * 0.12}s;--fs:${Math.max(14, d * 0.24)}px;width:${d}px;height:${d}px;left:${left}px;top:${top}px">
          <b class="num">${fmt(c.count)}</b><span>${esc(c.label)}</span></div>`;
    })
    .join('');
}
