import { makeBackground, type Palette } from './background';
import { slideDurationMultiplier } from './constants';
import { esc } from './html';
import type { Opts, SlideDef } from './interfaces';
import { makeScope, type Scope } from './scope';
import { buildSlides } from './slides';

export const skeleton = (): string => `
  <canvas class="recap__bg" aria-hidden="true"></canvas>
  <div class="blobs" aria-hidden="true">
    <div class="blob blob--a"></div>
    <div class="blob blob--b"></div>
    <div class="blob blob--c"></div>
  </div>
  <div class="grain" aria-hidden="true"></div>
  <header class="hud">
    <div class="progress"></div>
    <div class="hud__bar">
      <a href="/" class="hud__home"><img class="hud__logo" src="${site.asset.url('logo/lichess-white.svg')}" alt="Lichess"></a>
      <span class="hud__title"></span>
      <button class="hud__btn hud__pause" aria-label="${esc(i18n.site.pause)}">
        <svg viewBox="0 0 24 24"><path class="i-pause" d="M7 5h3v14H7zM14 5h3v14h-3z" /><path class="i-play" d="M8 5l11 7-11 7z" /></svg>
      </button>
    </div>
  </header>
  <main class="stage" aria-live="polite"></main>
  <button class="nav nav--prev" aria-label="Previous"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15.5 5l-7 7 7 7" /></svg></button>
  <button class="nav nav--next" aria-label="${esc(i18n.site.next)}"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8.5 5l7 7-7 7" /></svg></button>`;

export function play(root: HTMLElement, opts: Opts): void {
  const $in = <E extends Element = HTMLElement>(s: string) => root.querySelector<E>(s)!;
  const stage = $in('.stage');
  const progress = $in('.progress');
  const bg = makeBackground($in<HTMLCanvasElement>('.recap__bg'));
  let idx = -1;
  let current: { el: HTMLElement; scope: Scope } | undefined;
  let elapsed = 0;
  let last = performance.now();
  let paused = false;
  let holding = false;

  const go = (i: number, force = false) => {
    if (i < 0 || i >= slides.length || (i === idx && !force)) return;
    const back = i < idx;
    if (current) {
      const old = current;
      old.scope.dispose();
      old.el.classList.add('slide--leaving');
      setTimeout(() => old.el.remove(), 500);
    }
    idx = i;
    elapsed = 0;
    const def = slides[i];
    setTheme(def.theme);
    bg.warp(back ? 0.4 : 1);
    bars.forEach((b, k) => (b.style.transform = `scaleX(${k < i ? 1 : 0})`));
    const el = document.createElement('section');
    el.className = `slide slide--${def.key}${back ? ' slide--back' : ''}${def.top ? ' slide--top' : ''}`;
    stage.appendChild(el);
    const scope = makeScope();
    def.render(el, scope);
    current = { el, scope };
  };

  const setTheme = (theme: Palette) => {
    ['--c1', '--c2', '--c3'].forEach((v, i) => root.style.setProperty(v, theme[i]));
    bg.setColors(theme);
  };

  const slides: SlideDef[] = buildSlides({ opts, bg, restart: () => go(0, true) });
  progress.innerHTML = slides.map(() => '<div class="progress__seg"><i></i></div>').join('');
  const bars = [...progress.querySelectorAll<HTMLElement>('i')];
  $in('.hud__title').textContent = i18n.recap.shareableTitle(opts.recap!.year);

  const tick = (now: number) => {
    const dt = now - last;
    last = now;
    const def = slides[idx];
    if (def && !paused && !holding) {
      elapsed += dt;
      const dur = (def.dur ?? 6000) * slideDurationMultiplier;
      bars[idx].style.transform = `scaleX(${dur === Infinity ? 1 : Math.min(1, elapsed / dur)})`;
      if (elapsed >= dur && idx < slides.length - 1) go(idx + 1);
    }
    requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);

  const setPaused = (p: boolean) => {
    paused = p;
    root.classList.toggle('paused', p);
  };

  // tap left/right to navigate, press and hold to pause
  let holdTimer: Timeout | undefined;
  let pressed = false;
  stage.addEventListener('pointerdown', e => {
    // taps on controls, or anywhere in the donate panel, must not change slide
    if ((e.target as HTMLElement).closest('a,button,.donate__panel,.donate__after')) return;
    pressed = true;
    holdTimer = setTimeout(() => (holding = true), 220);
  });
  const release = (e: PointerEvent) => {
    clearTimeout(holdTimer);
    if (!pressed) return;
    const wasHold = holding;
    holding = false;
    pressed = false;
    if (wasHold || e.type === 'pointercancel') return;
    go(e.clientX < innerWidth * 0.3 ? idx - 1 : idx + 1);
  };
  stage.addEventListener('pointerup', release);
  stage.addEventListener('pointercancel', release);
  $in('.nav--prev').onclick = () => go(idx - 1);
  $in('.nav--next').onclick = () => go(idx + 1);
  $in('.hud__pause').onclick = () => setPaused(!paused);
  addEventListener('keydown', e => {
    // let focused controls handle their own keys, e.g. Space on a button
    if ((e.target as HTMLElement).closest?.('input,textarea,select,button,a')) return;
    if (e.key === 'ArrowRight') go(idx + 1);
    else if (e.key === 'ArrowLeft') go(idx - 1);
    else if (e.key === ' ') {
      e.preventDefault();
      setPaused(!paused);
    } else if (e.key === 'Home') go(0);
    else if (e.key === 'End') go(slides.length - 1);
  });
  document.addEventListener('visibilitychange', () => document.hidden && setPaused(true));

  // #n deep-links to slide n
  const fromHash = () => {
    const n = parseInt(location.hash.slice(1), 10);
    go(Number.isFinite(n) ? Math.max(0, Math.min(slides.length - 1, n)) : 0, true);
  };
  addEventListener('hashchange', fromHash);
  fromHash();
}
