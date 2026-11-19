import { esc, fmt, gauss, pick, pieceSrc, rand } from './html';
import type { Scope } from './scope';

// phones have 3x screens, but moving pictures don't need every pixel of them
const coarse = matchMedia('(pointer: coarse)').matches;

const setupCanvas = (
  cv: HTMLCanvasElement,
  W = cv.clientWidth,
  H = cv.clientHeight,
  maxDpr = coarse ? 1 : 1.5,
) => {
  const ctx = cv.getContext('2d')!;
  const dpr = Math.min(devicePixelRatio || 1, maxDpr);
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
const shadowPad = 6;
const sprites = new Map<HTMLImageElement, HTMLCanvasElement>();
const sprite = (img: HTMLImageElement, size: number, dpr: number) => {
  let cv = sprites.get(img);
  if (!cv) {
    cv = document.createElement('canvas');
    cv.width = cv.height = (size + shadowPad * 2) * dpr;
    const ctx = cv.getContext('2d')!;
    ctx.scale(dpr, dpr);
    ctx.shadowColor = 'rgba(0,0,0,.5)';
    ctx.shadowBlur = 4;
    ctx.drawImage(img, shadowPad, shadowPad, size, size);
    sprites.set(img, cv);
  }
  return cv;
};

const drawPiece = (ctx: CanvasRenderingContext2D, p: Falling, size: number, dpr: number) => {
  if (!p.img.complete || !p.img.naturalWidth) return;
  const half = size / 2 + shadowPad;
  const cos = Math.cos(p.rot) * dpr;
  const sin = Math.sin(p.rot) * dpr;
  ctx.setTransform(cos, sin, -sin, cos, p.x * dpr, p.y * dpr);
  ctx.drawImage(sprite(p.img, size, dpr), -half, -half, half * 2, half * 2);
};

// pieces raining down and piling into a mound at the bottom of the screen
export function pilePieces(sc: Scope, cv: HTMLCanvasElement, wanted: number): void {
  const count = coarse ? Math.min(200, wanted) : wanted;
  const { ctx, W, H, dpr } = setupCanvas(cv);
  // pieces that landed are drawn once onto a canvas underneath, so each frame only draws the falling ones
  const settled = cv.cloneNode() as HTMLCanvasElement;
  cv.before(settled);
  const sctx = setupCanvas(settled, W, H).ctx;

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
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
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
    return spawned < count || falling.length > 0;
  });
}

// spiral galaxy of games with one glowing dot for the player.
// It is drawn once; css tilts, zooms and spins it, so no frame redraws its thousands of stars.
export function galaxy(box: HTMLElement, youLabel: string): void {
  const size = Math.round(Math.max(innerWidth, innerHeight) * 1.1);
  box.innerHTML = `<div class="galaxy__tilt"><div class="galaxy__spin" style="width:${size}px;height:${size}px">
      <canvas></canvas><i class="galaxy__you"><span>${esc(youLabel)}</span></i>
    </div></div>`;
  const cv = box.querySelector('canvas')!;
  // the dots are tiny and blurry anyway: skip high-dpi
  const { ctx } = setupCanvas(cv, size, size, 1);
  const c = size / 2;
  const R = size / 2;
  ctx.globalCompositeOperation = 'lighter';
  // fewer on phone-sized screens
  const stars = Math.round(4000 * Math.min(1, Math.max(0.5, (innerWidth * innerHeight) / (1280 * 800))));
  for (let i = 0; i < stars; i++) {
    const arm = Math.floor(Math.random() * 3);
    const d = Math.pow(Math.random(), 1.6);
    const a = arm * ((Math.PI * 2) / 3) + d * 5.2 + gauss() * 0.45 * (1 - d * 0.5);
    const hue = Math.round(rand(200, 320) / 20) * 20;
    const shade = Math.round(d * 6) / 6;
    ctx.fillStyle = `hsla(${hue},95%,${75 - shade * 20}%,${0.95 - shade * 0.4})`;
    const s = rand(0.8, 2.6);
    ctx.fillRect(c + Math.cos(a) * d * R, c + Math.sin(a) * d * R, s, s);
  }
  const core = ctx.createRadialGradient(c, c, 0, c, c, R * 0.25);
  core.addColorStop(0, 'rgba(255,240,255,.8)');
  core.addColorStop(1, 'rgba(255,240,255,0)');
  ctx.fillStyle = core;
  ctx.fillRect(0, 0, size, size);
  const you = box.querySelector<HTMLElement>('.galaxy__you')!;
  const yd = 0.38 * R;
  const ya = 2.1;
  you.style.left = `${c + Math.cos(ya) * yd}px`;
  you.style.top = `${c + Math.sin(ya) * yd}px`;
}

// rough circle packing: biggest in the middle, others spiral outwards
export function layoutBubbles(box: HTMLElement, entries: [string, number][]): void {
  const max = entries[0][1];
  const palette = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#ec4899', '#14b8a6', '#6366f1'];
  const circles: { label: string; count: number; r: number; x: number; y: number }[] = [];
  for (const [label, count] of entries) {
    // big enough for a short label even when the count is tiny
    const r = Math.max(0.46, Math.sqrt(count / max));
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
  box.querySelectorAll<HTMLElement>('.bubble').forEach(fitInBubble);
}

// shrink the number and the label until they fit in the square inside the circle,
// since labels like "Simultaneous exhibitions" can be longer than a small bubble is wide
function fitInBubble(bubble: HTMLElement) {
  const num = bubble.querySelector<HTMLElement>('b')!;
  const label = bubble.querySelector<HTMLElement>('span')!;
  const side = bubble.clientWidth * 0.7;
  const shrink = (el: HTMLElement, fits: () => boolean, min: number) => {
    let size = parseFloat(getComputedStyle(el).fontSize);
    while (!fits() && size > min) el.style.fontSize = `${(size = Math.max(min, size * 0.92))}px`;
  };
  shrink(num, () => num.scrollWidth <= side, 12);
  shrink(
    label,
    () => label.scrollWidth <= label.clientWidth && num.offsetHeight + label.offsetHeight <= side,
    8,
  );
}
