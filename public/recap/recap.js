(() => {
  'use strict';

  /* ================= helpers ================= */

  const $ = s => document.querySelector(s);
  const nf = new Intl.NumberFormat('en');
  const fmt = n => nf.format(Math.round(n));
  const esc = s =>
    String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
  const easeOutExpo = t => (t >= 1 ? 1 : 1 - Math.pow(2, -10 * t));
  const easeInOut = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
  const rand = (a, b) => a + Math.random() * (b - a);
  const gauss = () => (Math.random() + Math.random() + Math.random() + Math.random() - 2) / 2;
  const pick = arr => arr[Math.floor(Math.random() * arr.length)];
  const asset = p => `../${p}`;
  const pieceSrc = (c, t) => asset(`piece/cburnett/${c}${t}.svg`);
  const flairSrc = f => asset(`flair/img/${f}.webp`);
  const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

  const words = (text, delay = 0, step = 0.06) =>
    text
      .split(' ')
      .map((w, i) => `<span class="word" style="--d:${(delay + i * step).toFixed(2)}s">${esc(w)}</span>`)
      .join(' ');

  const percent = (x, digits = 2) =>
    new Intl.NumberFormat('en', { style: 'percent', maximumFractionDigits: digits }).format(x);

  const formatDuration = (s, max = 2) => {
    const d = Math.floor(s / 86400),
      h = Math.floor((s % 86400) / 3600),
      m = Math.floor((s % 3600) / 60);
    const parts = [];
    if (d) parts.push(`${d} day${d > 1 ? 's' : ''}`);
    parts.push(`${h} hour${h !== 1 ? 's' : ''}`, `${m} minute${m !== 1 ? 's' : ''}`);
    return parts.slice(0, max).join(', ');
  };

  const perfMeta = {
    ultraBullet: ['UltraBullet', ''],
    bullet: ['Bullet', ''],
    blitz: ['Blitz', ''],
    rapid: ['Rapid', ''],
    classical: ['Classical', ''],
    correspondence: ['Correspondence', ''],
    standard: ['Standard', ''],
    chess960: ['Chess960', ''],
    kingOfTheHill: ['King of the Hill', ''],
    antichess: ['Antichess', ''],
    atomic: ['Atomic', ''],
    threeCheck: ['Three-check', ''],
    horde: ['Horde', ''],
    racingKings: ['Racing Kings', ''],
    crazyhouse: ['Crazyhouse', ''],
  };
  const variants = ['chess960', 'kingOfTheHill', 'antichess', 'atomic', 'threeCheck', 'horde', 'racingKings', 'crazyhouse'];

  const sourceNames = {
    pool: 'Quick pairing',
    lobby: 'Lobby',
    arena: 'Arenas',
    swiss: 'Swiss',
    friend: 'Challenges',
    ai: 'Computer',
    simul: 'Simuls',
  };

  const TOTAL_LICHESS_GAMES = 1_825_000_000;
  const PIECE_GRAMS = 41;

  /* ================= background: starfield + sparks ================= */

  const Bg = (() => {
    const cv = $('#bg');
    const ctx = cv.getContext('2d');
    let W = 0,
      H = 0,
      dpr = 1;
    let warp = 0;
    let colors = ['#7c3aed', '#06b6d4', '#f43f5e'];
    const stars = [];
    const sparks = [];

    const resize = () => {
      dpr = Math.min(devicePixelRatio || 1, 2);
      W = innerWidth;
      H = innerHeight;
      cv.width = W * dpr;
      cv.height = H * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    addEventListener('resize', resize);

    const newStar = z => ({ x: rand(-1, 1), y: rand(-1, 1), z: z ?? rand(0.05, 1), pz: 0 });
    for (let i = 0; i < 260; i++) stars.push(newStar());

    const frame = () => {
      ctx.clearRect(0, 0, W, H);
      const cx = W / 2,
        cy = H / 2,
        fov = Math.max(W, H) * 0.5;
      const speed = 0.0009 + warp * 0.03;
      warp *= 0.94;
      for (const s of stars) {
        s.pz = s.z;
        s.z -= speed;
        if (s.z <= 0.02) Object.assign(s, newStar(1), { pz: 1 });
        const x = cx + (s.x / s.z) * fov,
          y = cy + (s.y / s.z) * fov;
        const px = cx + (s.x / s.pz) * fov,
          py = cy + (s.y / s.pz) * fov;
        const a = Math.min(1, (1 - s.z) * 1.4);
        ctx.strokeStyle = `rgba(255,255,255,${a})`;
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

    const burst = (x, y, n = 90, palette = colors) => {
      if (reduceMotion) return;
      for (let i = 0; i < n; i++) {
        const a = rand(0, Math.PI * 2),
          v = rand(1.5, 7.5);
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
      const ids = [];
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
  })();

  /* ================= slide engine ================= */

  const setTheme = ([c1, c2, c3]) => {
    const r = document.documentElement.style;
    r.setProperty('--c1', c1);
    r.setProperty('--c2', c2);
    r.setProperty('--c3', c3);
    Bg.setColors([c1, c2, c3]);
  };

  // per-slide scope: timers and loops are cancelled automatically when the slide leaves
  const makeScope = () => {
    const timers = [],
      cleanups = [];
    let alive = true;
    return {
      after(ms, fn) {
        timers.push(setTimeout(() => alive && fn(), ms));
      },
      loop(fn) {
        const t0 = performance.now();
        const tick = now => {
          if (!alive) return;
          if (fn(now - t0) !== false) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      },
      onDispose(fn) {
        cleanups.push(fn);
      },
      dispose() {
        alive = false;
        timers.forEach(clearTimeout);
        cleanups.forEach(f => f());
      },
      get alive() {
        return alive;
      },
    };
  };

  const countUp = (scope, el, to, { dur = 2200, delay = 0, format = fmt } = {}) => {
    if (!el) return;
    el.textContent = format(0);
    scope.after(delay, () =>
      scope.loop(t => {
        const k = easeOutExpo(Math.min(1, t / dur));
        el.textContent = format(to * k);
        return k < 1;
      }),
    );
  };
  const countAll = (scope, root, opts) =>
    root.querySelectorAll('[data-count]').forEach(el =>
      countUp(scope, el, parseFloat(el.dataset.count), {
        ...opts,
        delay: parseFloat(el.dataset.delay || '0') * 1000,
      }),
    );

  /* ================= chess board view ================= */

  const makeBoard = (orientation = 'white') => {
    const pos = new MiniChess.Position();
    const wrap = document.createElement('div');
    wrap.className = 'board-wrap';
    const board = document.createElement('div');
    board.className = 'board';
    wrap.appendChild(board);
    const hls = [0, 1].map(() => {
      const h = document.createElement('div');
      h.className = 'board__hl';
      board.appendChild(h);
      return h;
    });
    const coords = sq => {
      const f = MiniChess.fileOf(sq),
        r = MiniChess.rankOf(sq);
      return orientation === 'white' ? [f, 7 - r] : [7 - f, r];
    };
    const at = (node, sq) => {
      const [x, y] = coords(sq);
      node.style.transform = `translate(${x * 100}%, ${y * 100}%)`;
    };
    const nodes = new Map();
    for (const p of pos.pieces()) {
      const n = document.createElement('div');
      n.className = 'board__piece';
      n.style.backgroundImage = `url(${pieceSrc(p.c, p.t)})`;
      at(n, p.sq);
      board.appendChild(n);
      nodes.set(p.id, n);
    }
    const play = san => {
      const r = pos.play(san);
      if (r.captured !== null) {
        const n = nodes.get(r.captured);
        setTimeout(() => n.classList.add('board__piece--gone'), 300);
        setTimeout(() => n.remove(), 700);
      }
      for (const m of r.moves) {
        const n = nodes.get(m.id);
        n.classList.add('board__piece--lift');
        at(n, m.to);
        setTimeout(() => n.classList.remove('board__piece--lift'), 560);
      }
      if (r.promoted) nodes.get(r.promoted.id).style.backgroundImage = `url(${pieceSrc(pos.turn === 'w' ? 'b' : 'w', r.promoted.t)})`;
      [r.from, r.to].forEach((sq, i) => {
        at(hls[i], sq);
        hls[i].style.opacity = 1;
      });
      return r;
    };
    return { el: wrap, play };
  };

  /* ================= slides ================= */

  const buildSlides = (opts, start) => {
    const R = opts.recap;
    const U = opts.user;
    const G = R.games;
    const name = (U.title ? U.title + ' ' : '') + U.name;
    const S = [];

    S.push({
      key: 'intro',
      title: `${R.year} Recap`,
      theme: ['#7c3aed', '#06b6d4', '#f43f5e'],
      dur: 6500,
      render(el, sc) {
        const kinds = ['wK', 'bQ', 'wN', 'bR', 'wB', 'bP', 'wQ', 'bN', 'wR', 'bB', 'wP', 'bK'];
        el.innerHTML = `
          <div class="intro__orbit"><div class="intro__ring">
            ${kinds.map(k => `<img src="${pieceSrc(k[0], k[1])}" alt="">`).join('')}
          </div></div>
          <img class="intro__logo reveal" src="${asset('logo/lichess-white.svg')}" alt="Lichess">
          <p class="kicker reveal" style="--d:.25s">Hi, ${esc(name)}</p>
          <h1 class="intro__year">${String(R.year)
            .split('')
            .map((d, i) => `<span style="--d:${0.5 + i * 0.12}s">${d}</span>`)
            .join('')}</h1>
          <p class="headline">${words("What a chess year you've had!", 1.3)}</p>`;
        const imgs = [...el.querySelectorAll('.intro__ring img')];
        sc.loop(t => {
          const { W, H } = Bg.size();
          const rx = Math.min(W * 0.44, 520),
            ry = Math.min(H * 0.3, 260);
          imgs.forEach((img, i) => {
            const a = t / 4200 + (i / imgs.length) * Math.PI * 2;
            const z = Math.sin(a); // -1 back, 1 front
            const s = 0.55 + (z + 1) * 0.4;
            img.style.transform = `translate(${Math.cos(a) * rx}px, ${z * ry}px) scale(${s}) rotate(${Math.cos(a) * 18}deg)`;
            img.style.zIndex = z > 0 ? 2 : -1;
            img.style.opacity = 0.35 + (z + 1) * 0.3;
            img.style.filter = `blur(${Math.max(0, -z * 2.5)}px) drop-shadow(0 10px 25px rgba(0,0,0,.6))`;
          });
        });
        sc.after(1600, () => sc.onDispose(Bg.fireworks(6, 2200)));
      },
    });

    if (!G.nbs.total) {
      S.push({
        key: 'no-games',
        title: 'Games',
        theme: ['#475569', '#06b6d4', '#7c3aed'],
        render(el) {
          el.innerHTML = `<p class="headline">${words('You did not play any games this year.')}</p>
            <a class="cta reveal" style="--d:.8s" href="/" target="_blank">Wanna play now?</a>`;
        },
      });
    } else {
      // ---- number of games + win ring
      S.push({
        key: 'games',
        title: 'Games',
        theme: ['#2563eb', '#22d3ee', '#a855f7'],
        render(el, sc) {
          const C = 2 * Math.PI * 88;
          const winRate = G.nbs.win / G.nbs.total;
          el.innerHTML = `
            <p class="kicker reveal">This year you played</p>
            <p class="mega num" data-count="${G.nbs.total}">0</p>
            <p class="headline reveal" style="--d:.3s">games</p>
            <div class="ring reveal" style="--d:.6s">
              <svg viewBox="0 0 200 200">
                <defs><linearGradient id="ring-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="var(--c2)"/><stop offset="1" stop-color="var(--c3)"/>
                </linearGradient></defs>
                <circle class="track" cx="100" cy="100" r="88"/>
                <circle class="bar" cx="100" cy="100" r="88" stroke-dasharray="${C}" stroke-dashoffset="${C}"/>
              </svg>
              <div class="ring__inner"><b class="num" id="winrate">0%</b><small class="small">win rate</small></div>
            </div>
            ${G.nbs.win ? `<p class="lead reveal" style="--d:1.2s">And you won <strong class="num" data-count="${G.nbs.win}" data-delay="1.2">0</strong>!</p>` : ''}
            <p class="small reveal" style="--d:2.2s">What did it take to get there?</p>`;
          countAll(sc, el);
          sc.after(700, () => {
            el.querySelector('.bar').style.strokeDashoffset = C * (1 - winRate);
            countUp(sc, el.querySelector('#winrate'), winRate * 100, { dur: 2400, format: v => Math.round(v) + '%' });
          });
          sc.after(3000, () => {
            const r = el.querySelector('.ring').getBoundingClientRect();
            Bg.burst(r.left + r.width / 2, r.top + r.height / 2, 120);
          });
        },
      });

      // ---- time played
      S.push({
        key: 'time',
        title: 'Time',
        theme: ['#0ea5e9', '#a3e635', '#14b8a6'],
        render(el, sc) {
          const s = G.timePlaying;
          const days = s / 86400;
          const d = Math.floor(days),
            h = Math.floor((s % 86400) / 3600),
            m = Math.floor((s % 3600) / 60);
          const lotr = s / 41160; // extended Lord of the Rings trilogy
          const verdict =
            days > 10 ? 'Now that is some serious dedication!' : days > 5 ? 'Every one of those minutes counted.' : 'Time well spent, one move at a time.';
          el.innerHTML = `
            <p class="kicker reveal">Time spent playing</p>
            <div class="clock reveal" style="--d:.1s"><div class="clock__face">
              ${Array.from({ length: 12 }, (_, i) => `<i class="clock__tick" style="transform:rotate(${i * 30}deg)"></i>`).join('')}
              <i class="clock__hand clock__hand--h"></i><i class="clock__hand clock__hand--m"></i><i class="clock__pin"></i>
            </div></div>
            <div class="time-units">
              ${d ? `<div class="reveal" style="--d:.3s"><b data-count="${d}">0</b><small>days</small></div>` : ''}
              <div class="reveal" style="--d:.45s"><b data-count="${h}">0</b><small>hours</small></div>
              <div class="reveal" style="--d:.6s"><b data-count="${m}">0</b><small>minutes</small></div>
            </div>
            <p class="lead reveal" style="--d:1.6s">${verdict}</p>
            ${lotr >= 1 ? `<p class="small reveal" style="--d:2.2s">You could have watched the extended <em>Lord of the Rings</em> trilogy <span class="hl">${fmt(lotr)} times</span> 🍿</p>` : ''}
            <p class="small reveal" style="--d:2.8s">How many moves did you play in all that time?</p>`;
          countAll(sc, el, { dur: 2600 });
          const hh = el.querySelector('.clock__hand--h'),
            mm = el.querySelector('.clock__hand--m');
          const turns = Math.min(60, Math.max(6, days * 3));
          sc.loop(t => {
            const k = easeOutExpo(Math.min(1, t / 3200));
            mm.style.transform = `rotate(${k * turns * 360}deg)`;
            hh.style.transform = `rotate(${(k * turns * 360) / 12}deg)`;
            return k < 1;
          });
        },
      });

      // ---- moves + pile of wood
      S.push({
        key: 'moves',
        title: 'Moves',
        theme: ['#d97706', '#f59e0b', '#ef4444'],
        dur: 7000,
        top: true,
        render(el, sc) {
          const grams = G.moves * PIECE_GRAMS;
          const kg = grams / 1000;
          const pianos = kg / 450;
          el.innerHTML = `
            <canvas class="fill"></canvas>
            <p class="kicker reveal">You played</p>
            <p class="mega num" data-count="${G.moves}">0</p>
            <p class="headline reveal" style="--d:.3s">moves</p>
            <p class="lead reveal" style="--d:1.4s">That's <strong class="num" data-count="${grams > 20000 ? Math.round(kg) : grams}" data-delay="1.4">0</strong> <strong>${grams > 20000 ? 'kg' : 'g'}</strong> of wood pushed!</p>
            ${pianos >= 1 ? `<p class="small reveal" style="--d:2.2s">About the weight of <span class="hl">${fmt(pianos)} grand piano${pianos >= 2 ? 's' : ''}</span> 🎹</p>` : ''}
            <p class="small reveal" style="--d:2.6s">Standard pieces weigh about 40g each</p>`;
          countAll(sc, el, { dur: 2600 });
          pilePieces(sc, el.querySelector('canvas'), Math.min(360, 80 + Math.sqrt(G.moves) * 0.9));
        },
      });

      // ---- perfs
      if (G.perfs.length)
        S.push({
          key: 'perfs',
          title: 'Time controls',
          theme: ['#db2777', '#f472b6', '#8b5cf6'],
          render(el, sc) {
            const max = Math.max(...G.perfs.map(p => p.games));
            el.innerHTML = `
              <p class="headline">${words('What did you play?')}</p>
              <div class="bars">${G.perfs
                .slice(0, 8)
                .map((p, i) => {
                  const [n, icon] = perfMeta[p.key] || [p.key, ''];
                  return `<div class="bars__row" style="--d:${0.3 + i * 0.14}s">
                    <i class="icon bars__icon">${icon}</i>
                    <div class="bars__track">
                      <div class="bars__fill" data-w="${p.games / max}" style="--d:${0.4 + i * 0.14}s"></div>
                      <div class="bars__label"><span>${esc(n)}</span><span class="num" data-count="${p.games}" data-delay="${0.4 + i * 0.14}">0</span></div>
                    </div>
                  </div>`;
                })
                .join('')}</div>`;
            countAll(sc, el, { dur: 1800 });
            sc.after(50, () =>
              el.querySelectorAll('.bars__fill').forEach(b => (b.style.transform = `scaleX(${Math.max(0.04, b.dataset.w)})`)),
            );
          },
        });

      // ---- sources
      const sources = Object.entries(G.sources)
        .filter(([, c]) => c > 0)
        .sort((a, b) => b[1] - a[1]);
      if (sources.length)
        S.push({
          key: 'sources',
          title: 'Sources',
          theme: ['#059669', '#34d399', '#0ea5e9'],
          render(el) {
            el.innerHTML = `<p class="headline">${words('Where did you find games?')}</p><div class="bubbles"></div>`;
            const box = el.querySelector('.bubbles');
            layoutBubbles(box, sources);
          },
        });

      // ---- opponents
      if (G.opponents.length)
        S.push({
          key: 'opponents',
          title: 'Rivals',
          theme: ['#dc2626', '#f97316', '#7c3aed'],
          render(el, sc) {
            const [top, ...rest] = G.opponents;
            el.innerHTML = `
              <p class="kicker reveal">Your best chess foe</p>
              <div class="versus">
                <div class="fighter fighter--l"><div class="avatar avatar--you"><span>${esc(U.name[0].toUpperCase())}</span></div>${esc(name)}</div>
                <div class="vs">VS</div>
                <a class="fighter fighter--r" href="/@/${esc(top.value.name)}" target="_blank">
                  <div class="avatar"><img src="${flairSrc(flairOf(top.value))}" alt=""></div>${userLabel(top.value)}
                </a>
              </div>
              <p class="lead reveal" style="--d:1.3s"><strong class="num" data-count="${top.count}" data-delay="1.3">0</strong> games together</p>
              ${
                rest.length
                  ? `<ol class="rivals">${rest
                      .map(
                        (o, i) => `<li style="--d:${1.9 + i * 0.15}s">
                        <a href="/@/${esc(o.value.name)}" target="_blank"><span class="rank">${i + 2}</span><img src="${flairSrc(flairOf(o.value))}" alt="">${userLabel(o.value)}</a>
                        <span class="num">${fmt(o.count)} games</span></li>`,
                      )
                      .join('')}</ol>`
                  : ''
              }`;
            countAll(sc, el, { dur: 1500 });
            sc.after(950, () => {
              const r = el.querySelector('.vs').getBoundingClientRect();
              Bg.burst(r.left + r.width / 2, r.top + r.height / 2, 80, ['#f97316', '#fde047', '#ef4444']);
              Bg.warp(0.6);
            });
          },
        });

      // ---- first move
      const first = G.firstMoves[0];
      if (first && G.nbWhite)
        S.push({
          key: 'first',
          title: 'First move',
          theme: ['#4f46e5', '#818cf8', '#22d3ee'],
          render(el, sc) {
            const board = makeBoard('white');
            el.innerHTML = `
              <p class="kicker reveal">Your signature first move</p>
              <p class="mega mega--sm reveal">1. ${esc(first.value)}</p>`;
            el.appendChild(board.el);
            el.insertAdjacentHTML(
              'beforeend',
              `<p class="lead reveal" style="--d:1.5s">is how you started <strong class="num" data-count="${first.count}" data-delay="1.5">0</strong> (${percent(first.count / G.nbWhite)}) of your games as white</p>
              ${
                G.firstMoves.length > 1
                  ? `<div class="first-moves">${G.firstMoves
                      .slice(1, 5)
                      .map((f, i) => `<span class="chip reveal" style="--d:${2.2 + i * 0.12}s">1. ${esc(f.value)} <span class="small num">${fmt(f.count)}</span></span>`)
                      .join('')}</div>`
                  : ''
              }`,
            );
            countAll(sc, el, { dur: 1600 });
            sc.after(1300, () => {
              try {
                board.play(first.value);
              } catch (e) {
                console.warn(e);
              }
            });
          },
        });

      // ---- openings
      for (const color of ['white', 'black']) {
        const o = G.openings[color];
        if (!o || !o.count) continue;
        S.push({
          key: 'opening-' + color,
          title: `Opening as ${color}`,
          theme: color === 'white' ? ['#e2e8f0', '#38bdf8', '#a78bfa'] : ['#4338ca', '#818cf8', '#ec4899'],
          dur: 8500,
          render(el, sc) {
            const board = makeBoard(color);
            const moves = MiniChess.parsePgn(o.value.pgn);
            el.innerHTML = `<p class="kicker reveal">Your favourite opening as ${color}</p>
              <p class="headline"><a class="typed" href="/opening/${esc(o.value.key)}" target="_blank" style="text-decoration:none"></a></p>`;
            el.appendChild(board.el);
            el.insertAdjacentHTML(
              'beforeend',
              `<div class="movelist"></div>
              <p class="lead reveal" style="--d:1s">Played in <strong class="num" data-count="${o.count}" data-delay="1">0</strong> of your games as ${color}</p>`,
            );
            countAll(sc, el, { dur: 1600 });
            typeText(sc, el.querySelector('.typed'), o.value.name, 300);
            const list = el.querySelector('.movelist');
            const step = Math.min(700, 5200 / Math.max(1, moves.length));
            moves.forEach((san, i) =>
              sc.after(1100 + i * step, () => {
                if (i % 2 === 0) list.insertAdjacentHTML('beforeend', `<span class="n">${i / 2 + 1}.</span>`);
                list.insertAdjacentHTML('beforeend', `<span>${esc(san)}</span>`);
                try {
                  board.play(san);
                } catch (e) {
                  console.warn(e);
                }
              }),
            );
          },
        });
      }
    }

    // ---- puzzles
    const P = R.puzzles;
    S.push({
      key: 'puzzles',
      title: 'Puzzles',
      theme: ['#16a34a', '#4ade80', '#facc15'],
      dur: 6500,
      render(el, sc) {
        if (!P.nbs.total) {
          el.innerHTML = `<p class="headline">${words('You did not solve any puzzles this year.')}</p>
            <a class="cta reveal" style="--d:.8s" href="/training" target="_blank">Wanna try some now?</a>`;
          return;
        }
        const cells = Math.min(P.nbs.total, 400);
        const cols = cells > 200 ? 25 : cells > 60 ? 15 : 10;
        const wins = Math.round((P.nbs.win / P.nbs.total) * cells);
        el.innerHTML = `
          <p class="kicker reveal">You solved</p>
          <p class="mega mega--sm num" data-count="${P.nbs.total}">0</p>
          <p class="headline reveal" style="--d:.2s">puzzles</p>
          <div class="mosaic" style="--cols:${cols}">${'<i></i>'.repeat(cells)}</div>
          <div class="legend reveal" style="--d:.6s"><span><i style="background:#34d399"></i>first try</span><span><i style="background:#fb923c"></i>needed another go</span></div>
          ${P.nbs.win ? `<p class="lead reveal" style="--d:2.4s">You won <strong class="num" data-count="${P.nbs.win}" data-delay="2.4">0</strong> of them on the first try!</p>` : ''}
          ${P.votes.nb ? `<p class="small reveal" style="--d:3s">Thank you for voting on ${fmt(P.votes.nb)} puzzles${P.votes.themes ? `, and helping tag ${fmt(P.votes.themes)} of them` : ''} 💚</p>` : ''}`;
        countAll(sc, el, { dur: 2400 });
        const nodes = [...el.querySelectorAll('.mosaic i')];
        const order = nodes.map((_, i) => i).sort(() => Math.random() - 0.5);
        const kinds = order.map((_, i) => (i < wins ? 'w' : 'l')).sort(() => Math.random() - 0.5);
        let done = 0;
        sc.after(400, () =>
          sc.loop(t => {
            const target = Math.floor(easeInOut(Math.min(1, t / 2200)) * cells);
            for (; done < target; done++) nodes[order[done]].className = kinds[done];
            return done < cells;
          }),
        );
      },
    });

    // ---- lichess games galaxy
    if (G.nbs.total)
      S.push({
        key: 'lichess-games',
        title: 'Lichess',
        theme: ['#312e81', '#c084fc', '#38bdf8'],
        dur: 6500,
        render(el, sc) {
          el.innerHTML = `
            <canvas class="fill"></canvas>
            <p class="mega mega--sm num" data-count="${TOTAL_LICHESS_GAMES}">0</p>
            <p class="headline">${words(`games played on Lichess in ${R.year}`, 0.3)}</p>
            <p class="lead reveal" style="--d:2.4s"><strong>${percent(G.nbs.total / TOTAL_LICHESS_GAMES, 6)}</strong> of them were yours. Every one counts ✨</p>`;
          countAll(sc, el, { dur: 2600 });
          galaxy(sc, el.querySelector('canvas'));
        },
      });

    // ---- malware
    S.push({
      key: 'malware',
      title: 'Privacy',
      theme: ['#0f766e', '#2dd4bf', '#84cc16'],
      render(el) {
        const bad = ['Ads', 'Trackers', 'Data brokers', 'Fingerprinting', 'Pop-ups', 'Paywalls', 'Dark patterns'];
        el.innerHTML = `
          <div class="shield reveal">
            <svg viewBox="0 0 100 110"><defs><linearGradient id="sg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--c2)"/><stop offset="1" stop-color="var(--c1)"/></linearGradient></defs>
              <path d="M50 4 L92 18 V52 C92 80 72 98 50 106 C28 98 8 80 8 52 V18 Z" fill="url(#sg)" fill-opacity=".25" stroke="url(#sg)" stroke-width="4"/></svg>
            <div class="shield__zero">0</div><div class="shield__scan"></div>
          </div>
          <p class="headline">${words('ads and trackers loaded', 0.3)}</p>
          <div class="blocked">${bad.map((b, i) => `<span style="--d:${0.6 + i * 0.12}s">${b}</span>`).join('')}</div>
          <ul class="checks">
            <li style="--d:2s">We didn't sell your personal data</li>
            <li style="--d:2.3s">We didn't use your device against you</li>
          </ul>
          <p class="small reveal" style="--d:2.8s">But other websites do, so please <a href="/ads" target="_blank">be careful</a>.</p>`;
      },
    });

    // ---- thanks
    S.push({
      key: 'thanks',
      title: 'Thanks',
      theme: ['#7c3aed', '#f43f5e', '#facc15'],
      render(el, sc) {
        el.innerHTML = `
          <img class="intro__logo reveal" style="width:110px;height:110px" src="${asset('logo/lichess-white.svg')}" alt="Lichess">
          <p class="headline">${words('Thank you for playing on Lichess!', 0.3)}</p>
          <p class="lead reveal" style="--d:1.2s">We're glad you're here. Have a great <strong>${R.year + 1}</strong>!</p>`;
        sc.onDispose(Bg.fireworks(4, 800));
        sc.after(1500, () => sc.onDispose(Bg.fireworks(8, 3500)));
      },
    });

    // ---- share card
    S.push({
      key: 'share',
      title: 'Share',
      theme: ['#7c3aed', '#06b6d4', '#f43f5e'],
      dur: 9000,
      render(el, sc) {
        const topPerf = G.perfs[0];
        const stat = (v, l, wide) =>
          `<div class="card__stat${wide ? ' card__stat--wide' : ''}"><b>${v}</b><small>${esc(l)}</small></div>`;
        el.innerHTML = `
          <div class="card">
            <div class="card__head"><img src="${asset('logo/lichess-white.svg')}" alt="">
              <div><h3>My ${R.year} recap</h3><small>${esc(name)} · lichess.org</small></div></div>
            <div class="card__grid">
              ${stat(fmt(G.nbs.total), 'games')}
              ${stat(fmt(G.moves), 'moves played')}
              ${stat(esc(formatDuration(G.timePlaying)), 'spent playing', true)}
              ${topPerf ? stat(`<i class="icon">${(perfMeta[topPerf.key] || [])[1] || ''}</i> ${esc((perfMeta[topPerf.key] || [topPerf.key])[0])}`, variants.includes(topPerf.key) ? 'favourite variant' : 'favourite time control') : ''}
              ${G.opponents[0] ? stat(userLabel(G.opponents[0].value), 'most played opponent') : ''}
              ${stat(fmt(R.puzzles.nbs.total), 'puzzles solved')}
              ${G.firstMoves[0] ? stat('1. ' + esc(G.firstMoves[0].value), 'favourite first move') : ''}
              ${['white', 'black']
                .filter(c => G.openings[c]?.count)
                .map(c => stat(esc(G.openings[c].value.name), 'as ' + c, true))
                .join('')}
            </div>
          </div>
          <div class="actions reveal" style="--d:1s">
            <button class="cta" data-act="share">Share</button>
            <button class="cta cta--ghost" data-act="replay">Replay ↺</button>
          </div>`;
        const card = el.querySelector('.card');
        const tilt = e => {
          const r = card.getBoundingClientRect();
          const x = (e.clientX - r.left) / r.width - 0.5,
            y = (e.clientY - r.top) / r.height - 0.5;
          card.style.setProperty('--ry', `${x * 16}deg`);
          card.style.setProperty('--rx', `${-y * 16}deg`);
          card.style.setProperty('--hx', `${50 + x * 100}%`);
          card.style.setProperty('--hy', `${50 + y * 100}%`);
        };
        // idle shimmer until the pointer takes over
        let touched = false;
        sc.loop(t => {
          if (touched) return false;
          tilt({
            clientX: card.getBoundingClientRect().left + card.offsetWidth * (0.5 + Math.sin(t / 1400) * 0.35),
            clientY: card.getBoundingClientRect().top + card.offsetHeight * (0.5 + Math.cos(t / 1700) * 0.3),
          });
        });
        const move = e => {
          touched = true;
          tilt(e);
        };
        addEventListener('pointermove', move);
        sc.onDispose(() => removeEventListener('pointermove', move));
        el.querySelector('[data-act=replay]').onclick = () => start(0);
        el.querySelector('[data-act=share]').onclick = async () => {
          const data = { title: `My ${R.year} Lichess recap`, url: location.href };
          try {
            if (navigator.share) await navigator.share(data);
            else {
              await navigator.clipboard.writeText(location.href);
              toast('Link copied!');
            }
          } catch {}
        };
        sc.after(600, () => {
          const r = card.getBoundingClientRect();
          Bg.burst(r.left + r.width / 2, r.top + r.height / 3, 140);
        });
      },
    });

    // ---- donate: hook, pre-selected tier, regional price anchor, impact
    S.push({
      key: 'donate',
      title: 'Support Lichess',
      theme: ['#be185d', '#f59e0b', '#f43f5e'],
      dur: Infinity,
      top: true,
      render: (el, sc) => renderDonate(el, sc, opts, start),
    });

    return S;
  };

  /* ================= donate slide ================= */

  // Lichess costs roughly $1.50 per minute of global uptime
  const UPTIME_USD_PER_MINUTE = 1.5;
  const TIERS = {
    monthly: [2.5, 5, 10], // minutes of uptime per month
    once: [30, 60, 120], // minutes of uptime, one-off
  };
  // What 5 minutes of Lichess looks like. PLACEHOLDER estimates, replace with real figures.
  const FIVE_MINUTES = {
    moves: Math.round((TOTAL_LICHESS_GAMES / (365 * 24 * 12)) * 70),
    games: Math.round(TOTAL_LICHESS_GAMES / (365 * 24 * 12)),
    players: 110_000,
    schools: 320,
  };
  // $7.50 around the world. In production the region would come from Cloudflare's country header.
  const PRICE_ANCHORS = [
    { region: 'na', flag: '🇺🇸', text: 'a Starbucks coffee in New York' },
    { region: 'eu', flag: '🇬🇧', text: 'two bus journeys in London' },
    { region: 'eu', flag: '🇫🇷', text: 'a glass of house wine in Paris' },
    { region: 'as', flag: '🇯🇵', text: 'a bowl of ramen in Tokyo' },
    { region: 'sa', flag: '🇧🇷', text: 'a plate of feijoada in São Paulo' },
    { region: 'af', flag: '🇿🇦', text: 'a cinema ticket in Cape Town' },
    { region: 'oc', flag: '🇦🇺', text: 'a flat white and a pastry in Melbourne' },
  ];
  const ANTARCTICA = { region: 'an', flag: '🐧', text: 'one premium fish at the McMurdo Station canteen' };

  const SOUTH_AMERICA =
    /^America\/(Sao_Paulo|Argentina|Buenos_Aires|Bogota|Lima|Santiago|Caracas|Montevideo|La_Paz|Asuncion|Guayaquil|Cayenne|Paramaribo|Guyana|Recife|Fortaleza|Belem|Manaus|Bahia)/;
  const detectRegion = () => {
    const forced = new URLSearchParams(location.search).get('region');
    if (forced) return forced;
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || '';
    if (tz.startsWith('Antarctica/')) return 'an';
    if (SOUTH_AMERICA.test(tz)) return 'sa';
    if (tz.startsWith('America/') || tz.startsWith('US/') || tz.startsWith('Canada/')) return 'na';
    if (tz.startsWith('Europe/') || tz === 'Atlantic/Reykjavik') return 'eu';
    if (tz.startsWith('Asia/')) return 'as';
    if (tz.startsWith('Africa/')) return 'af';
    if (tz.startsWith('Australia/') || tz.startsWith('Pacific/')) return 'oc';
    return 'na';
  };

  const usd = n =>
    new Intl.NumberFormat('en', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: n % 1 ? 2 : 0,
      maximumFractionDigits: 2,
    }).format(n);
  const minsLabel = m => (m >= 60 && m % 60 === 0 ? `${m / 60} hour${m > 60 ? 's' : ''}` : `${m} min${m === 1 ? '' : 's'}`);

  const renderDonate = (el, sc, opts, start) => {
    const minutes = Math.round(opts.recap.games.timePlaying / 60);
    const region = detectRegion();
    const anchors =
      region === 'an'
        ? [ANTARCTICA]
        : [...PRICE_ANCHORS.filter(a => a.region === region), ...PRICE_ANCHORS.filter(a => a.region !== region)];
    const state = { freq: 'monthly', tier: 1 };

    el.innerHTML = `
      <p class="kicker reveal">${opts.user.patron ? 'You are already a Patron 💖 One more thing…' : 'One more thing…'}</p>
      <p class="headline donate__hook reveal" style="--d:.15s">This year, you played around <span class="hl num" data-count="${minutes}" data-delay=".3">0</span> minutes on Lichess!</p>
      <p class="lead reveal" style="--d:.9s">If you've enjoyed them, consider donating <strong data-slot="pitch"></strong> of global uptime<span data-slot="every"></span>:</p>
      <div class="donate__panel reveal" style="--d:1.3s">
        <div class="freq" role="radiogroup" aria-label="Donation frequency">
          <i class="freq__pill"></i>
          <button role="radio" data-freq="monthly">Monthly</button>
          <button role="radio" data-freq="once">One-off</button>
        </div>
        <div class="tiers" role="radiogroup" aria-label="Amount"></div>
        <div class="donate__actions">
          <a class="cta donate__go" target="_blank" rel="noopener"></a>
          <button class="donate__skip">Not today</button>
        </div>
      </div>
      <div class="donate__after" hidden></div>
      <p class="anchor reveal" style="--d:1.8s">
        Keeping Lichess running for <b>5 minutes</b>, worldwide, costs us <b>${usd(5 * UPTIME_USD_PER_MINUTE)}</b>.
        That's about the same as <span class="ticker"></span>
      </p>
      <div class="impact reveal" style="--d:2.2s">
        <p class="small">In those 5 minutes, Lichess will see</p>
        <div class="impact__grid">
          <div><b class="num" data-count="${FIVE_MINUTES.moves}" data-delay="2.3">0</b><small>moves</small></div>
          <div><b class="num" data-count="${FIVE_MINUTES.games}" data-delay="2.3">0</b><small>games</small></div>
          <div><b class="num" data-count="${FIVE_MINUTES.players}" data-delay="2.3">0</b><small>players</small></div>
          <div><b class="num" data-count="${FIVE_MINUTES.schools}" data-delay="2.3">0</b><small>schools</small></div>
        </div>
      </div>`;
    countAll(sc, el, { dur: 2000 });

    const $in = s => el.querySelector(s);
    const tiersEl = $in('.tiers');
    const go = $in('.donate__go');

    const update = () => {
      const tierMins = TIERS[state.freq];
      const monthly = state.freq === 'monthly';
      $in('.freq').dataset.freq = state.freq;
      el.querySelectorAll('[data-freq]').forEach(b => b.setAttribute('aria-checked', b.dataset.freq === state.freq));
      tiersEl.innerHTML = tierMins
        .map(
          (m, i) => `<button role="radio" aria-checked="${i === state.tier}" class="tier${i === state.tier ? ' tier--on' : ''}" data-tier="${i}">
            ${i === 1 ? '<em>suggested</em>' : ''}
            <b>${usd(m * UPTIME_USD_PER_MINUTE)}</b>
            <small>${minsLabel(m)} of uptime ${monthly ? 'per month' : 'per year'}</small>
          </button>`,
        )
        .join('');
      const m = tierMins[state.tier];
      const amount = m * UPTIME_USD_PER_MINUTE;
      $in('[data-slot=pitch]').textContent = minsLabel(m).replace(/\bmins?\b/, w => (w === 'min' ? 'minute' : 'minutes'));
      $in('[data-slot=every]').textContent = monthly ? ', every month' : '';
      go.textContent = `Donate ${usd(amount)}${monthly ? ' / month' : ''}`;
      // the patron page does not read these yet: it would need to pre-select the plan
      go.href = `/patron?freq=${monthly ? 'monthly' : 'onetime'}&amount=${amount}`;
    };
    update();

    el.querySelectorAll('[data-freq]').forEach(
      b =>
        (b.onclick = () => {
          state.freq = b.dataset.freq;
          update();
        }),
    );
    tiersEl.addEventListener('click', e => {
      const b = e.target.closest('[data-tier]');
      if (!b) return;
      state.tier = +b.dataset.tier;
      update();
    });

    const after = (html, celebrate) => {
      $in('.donate__panel').hidden = true;
      const box = $in('.donate__after');
      box.hidden = false;
      box.innerHTML = html;
      box.querySelector('[data-act=replay]').onclick = () => start(0);
      const back = box.querySelector('[data-act=back]');
      if (back)
        back.onclick = () => {
          box.hidden = true;
          $in('.donate__panel').hidden = false;
        };
      if (celebrate) {
        const r = box.getBoundingClientRect();
        Bg.burst(r.left + r.width / 2, r.top + r.height / 2, 160, ['#f59e0b', '#f43f5e', '#fde047']);
        sc.onDispose(Bg.fireworks(6, 2000));
      }
    };
    go.addEventListener('click', () =>
      after(
        `<p class="headline">💖</p>
         <p class="lead">Thank you for supporting chess for everyone. All forms of support from the community are vital for us!</p>
         <button class="cta cta--ghost" data-act="replay">Replay my recap ↺</button>`,
        true,
      ),
    );
    $in('.donate__skip').onclick = () =>
      after(
        `<p class="lead">No worries! Thank you for playing on Lichess. All forms of support from the community are vital for us, and that includes just being here 💜</p>
         <div class="donate__actions">
           <button class="cta cta--ghost" data-act="replay">Replay my recap ↺</button>
           <button class="donate__skip" data-act="back">Changed my mind</button>
         </div>`,
        false,
      );

    const ticker = $in('.ticker');
    let n = 0;
    const show = () => {
      const a = anchors[n++ % anchors.length];
      ticker.innerHTML = `<span class="ticker__item">${a.flag} <b>${esc(a.text)}</b></span>`;
      if (anchors.length > 1) sc.after(n === 1 ? 4500 : 2800, show);
    };
    show();
  };

  /* ================= slide-specific visuals ================= */

  const flairCache = new Map();
  const flairOf = u =>
    u.flair ||
    flairCache.get(u.id) ||
    flairCache.set(u.id, pick(['activity.lichess-horsey', 'activity.lichess-hogger', 'activity.lichess-horsey-yin-yang'])).get(u.id);

  const userLabel = u => `${u.title ? `<span class="title">${esc(u.title)}</span>` : ''}${esc(u.name)}`;

  const typeText = (sc, el, text, delay = 0) => {
    sc.after(delay, () => {
      let i = 0;
      sc.loop(t => {
        const n = Math.min(text.length, Math.floor(t / 38));
        if (n !== i) el.textContent = text.slice(0, (i = n));
        if (n >= text.length) {
          sc.after(1600, () => el.classList.remove('typed'));
          return false;
        }
      });
    });
  };

  const toast = msg => {
    const t = document.createElement('div');
    t.className = 'toast';
    t.textContent = msg;
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 1800);
  };

  // pieces raining down and piling into a mound at the bottom of the screen
  const pilePieces = (sc, cv, count) => {
    const ctx = cv.getContext('2d');
    const dpr = Math.min(devicePixelRatio || 1, 2);
    const W = cv.clientWidth,
      H = cv.clientHeight;
    cv.width = W * dpr;
    cv.height = H * dpr;
    ctx.scale(dpr, dpr);
    const settled = document.createElement('canvas');
    settled.width = cv.width;
    settled.height = cv.height;
    const sctx = settled.getContext('2d');
    sctx.scale(dpr, dpr);

    const kinds = ['P', 'P', 'P', 'P', 'N', 'B', 'R', 'Q', 'K'];
    const imgs = {};
    for (const c of 'wb') for (const t of 'PNBRQK') {
      const i = new Image();
      i.src = pieceSrc(c, t);
      imgs[c + t] = i;
    }
    const size = Math.max(26, Math.min(48, W / 26));
    const colW = size * 0.55;
    const cols = new Array(Math.ceil(W / colW) + 1).fill(0);
    const maxH = H * 0.26;
    const falling = [];
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
          img: imgs[pick('wb') + pick(kinds)],
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
          drawPiece(sctx, p, size);
          const c = p.col;
          cols[c] = Math.min(maxH, cols[c] + size * 0.42);
          for (let k = 1; k < 5; k++) {
            const lim = cols[c] - k * size * 0.32;
            if (cols[c - k] !== undefined) cols[c - k] = Math.min(maxH, Math.max(cols[c - k], lim));
            if (cols[c + k] !== undefined) cols[c + k] = Math.min(maxH, Math.max(cols[c + k], lim));
          }
          falling.splice(i, 1);
        } else drawPiece(ctx, p, size);
      }
      ctx.drawImage(settled, 0, 0, W, H);
      return spawned < count || falling.length > 0;
    });
  };
  const drawPiece = (ctx, p, size) => {
    if (!p.img.complete) return;
    ctx.save();
    ctx.translate(p.x, p.y);
    ctx.rotate(p.rot);
    ctx.shadowColor = 'rgba(0,0,0,.5)';
    ctx.shadowBlur = 8;
    ctx.drawImage(p.img, -size / 2, -size / 2, size, size);
    ctx.restore();
  };

  // spiral galaxy of games with one glowing dot for "you"
  const galaxy = (sc, cv) => {
    const ctx = cv.getContext('2d');
    const dpr = Math.min(devicePixelRatio || 1, 2);
    const W = cv.clientWidth,
      H = cv.clientHeight;
    cv.width = W * dpr;
    cv.height = H * dpr;
    ctx.scale(dpr, dpr);
    const R = Math.max(W, H) * 0.55;
    const pts = Array.from({ length: 4000 }, () => {
      const arm = Math.floor(Math.random() * 3);
      const d = Math.pow(Math.random(), 1.6);
      return {
        d,
        a: arm * ((Math.PI * 2) / 3) + d * 5.2 + gauss() * 0.45 * (1 - d * 0.5),
        s: rand(0.8, 2.6),
        hue: rand(200, 320),
      };
    });
    const you = { d: 0.38, a: 2.1 };
    sc.loop(t => {
      ctx.clearRect(0, 0, W, H);
      const zoom = 0.25 + easeOutExpo(Math.min(1, t / 3000)) * 0.75;
      const rot = t / 9000;
      const cx = W / 2,
        cy = H * 0.52;
      ctx.globalCompositeOperation = 'lighter';
      for (const p of pts) {
        const a = p.a + rot * (1.4 - p.d);
        const x = cx + Math.cos(a) * p.d * R * zoom,
          y = cy + Math.sin(a) * p.d * R * zoom * 0.55;
        ctx.fillStyle = `hsla(${p.hue},95%,${75 - p.d * 20}%,${0.95 - p.d * 0.4})`;
        ctx.fillRect(x, y, p.s, p.s);
      }
      const core = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * zoom * 0.25);
      core.addColorStop(0, 'rgba(255,240,255,.8)');
      core.addColorStop(1, 'rgba(255,240,255,0)');
      ctx.fillStyle = core;
      ctx.fillRect(0, 0, W, H);
      ctx.globalCompositeOperation = 'source-over';
      if (t > 2600) {
        const a = you.a + rot * (1.4 - you.d);
        const x = cx + Math.cos(a) * you.d * R * zoom,
          y = cy + Math.sin(a) * you.d * R * zoom * 0.55;
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
        ctx.fillText('you', x + 14, y - 12);
      }
    });
  };

  // rough circle packing: biggest in the middle, others spiral outwards
  const layoutBubbles = (box, entries) => {
    const max = entries[0][1];
    const palette = ['#10b981', '#0ea5e9', '#8b5cf6', '#f59e0b', '#ec4899', '#14b8a6', '#6366f1'];
    const circles = [];
    for (const [k, c] of entries) {
      const r = Math.max(0.34, Math.sqrt(c / max));
      let x = 0,
        y = 0;
      if (circles.length) {
        for (let d = 0.2; ; d += 0.04) {
          let ok = false;
          for (let a = 0; a < Math.PI * 2; a += 0.15) {
            x = Math.cos(a + d) * d * 1.3;
            y = Math.sin(a + d) * d * 0.9;
            if (circles.every(o => Math.hypot(o.x - x, o.y - y) > o.r + r + 0.05)) {
              ok = true;
              break;
            }
          }
          if (ok) break;
        }
      }
      circles.push({ k, c, r, x, y });
    }
    const minX = Math.min(...circles.map(c => c.x - c.r)),
      maxX = Math.max(...circles.map(c => c.x + c.r));
    const minY = Math.min(...circles.map(c => c.y - c.r)),
      maxY = Math.max(...circles.map(c => c.y + c.r));
    const bw = box.clientWidth,
      bh = box.clientHeight;
    const scale = Math.min(bw / (maxX - minX), bh / (maxY - minY));
    const ox = (bw - (maxX - minX) * scale) / 2,
      oy = (bh - (maxY - minY) * scale) / 2;
    box.innerHTML = circles
      .map((c, i) => {
        const d = c.r * 2 * scale;
        return `<div class="bubble" title="${esc(sourceNames[c.k] || c.k)}" style="
          --bc:${palette[i % palette.length]};--d:${0.3 + i * 0.12}s;--fs:${Math.max(14, d * 0.24)}px;
          width:${d}px;height:${d}px;left:${ox + (c.x - c.r - minX) * scale}px;top:${oy + (c.y - c.r - minY) * scale}px">
          <b class="num">${fmt(c.c)}</b><span>${esc(sourceNames[c.k] || c.k)}</span></div>`;
      })
      .join('');
  };

  /* ================= player ================= */

  const run = opts => {
    const root = $('#recap');
    const stage = $('#stage');
    const progress = $('#progress');
    const hudTitle = $('#hud-title');
    let slides = [];
    let idx = -1;
    let current = null; // { el, scope }
    let elapsed = 0;
    let last = performance.now();
    let paused = false;
    let holding = false;

    const start = i => go(i, true);
    slides = buildSlides(opts, start);
    progress.innerHTML = slides.map(() => '<div class="progress__seg"><i></i></div>').join('');
    const bars = [...progress.querySelectorAll('i')];

    const go = (i, force = false) => {
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
      Bg.warp(back ? 0.4 : 1);
      hudTitle.textContent = def.title;
      bars.forEach((b, k) => (b.style.transform = `scaleX(${k < i ? 1 : 0})`));
      const el = document.createElement('section');
      el.className = `slide slide--${def.key}${back ? ' slide--back' : ''}`;
      if (def.top) el.style.justifyContent = 'flex-start';
      stage.appendChild(el);
      const scope = makeScope();
      def.render(el, scope);
      current = { el, scope };
    };

    const tick = now => {
      const dt = now - last;
      last = now;
      const def = slides[idx];
      if (def && !paused && !holding) {
        elapsed += dt;
        const dur = def.dur ?? 6000;
        bars[idx].style.transform = `scaleX(${dur === Infinity ? 1 : Math.min(1, elapsed / dur)})`;
        if (elapsed >= dur && idx < slides.length - 1) go(idx + 1);
      }
      requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);

    const setPaused = p => {
      paused = p;
      root.classList.toggle('paused', p);
    };

    // tap left/right to navigate, press and hold to pause
    let holdTimer = 0,
      downAt = null;
    stage.addEventListener('pointerdown', e => {
      if (e.target.closest('a,button')) return;
      downAt = e;
      holdTimer = setTimeout(() => (holding = true), 220);
    });
    const release = e => {
      clearTimeout(holdTimer);
      if (!downAt) return;
      const wasHold = holding;
      holding = false;
      downAt = null;
      if (wasHold || e.type === 'pointercancel') return;
      e.clientX < innerWidth * 0.3 ? go(idx - 1) : go(idx + 1);
    };
    stage.addEventListener('pointerup', release);
    stage.addEventListener('pointercancel', release);
    $('#nav-prev').onclick = () => go(idx - 1);
    $('#nav-next').onclick = () => go(idx + 1);
    $('#btn-pause').onclick = () => setPaused(!paused);
    addEventListener('keydown', e => {
      if (e.key === 'ArrowRight') go(idx + 1);
      else if (e.key === 'ArrowLeft') go(idx - 1);
      else if (e.key === ' ') {
        e.preventDefault();
        setPaused(!paused);
      } else if (e.key === 'Home') go(0);
      else if (e.key === 'End') go(slides.length - 1);
    });
    document.addEventListener('visibilitychange', () => document.hidden && setPaused(true));

    // #n deep-links to slide n, handy while iterating on a single slide
    const fromHash = () => {
      const n = parseInt(location.hash.slice(1), 10);
      go(Number.isFinite(n) ? Math.max(0, Math.min(slides.length - 1, n)) : 0, true);
    };
    addEventListener('hashchange', fromHash);
    fromHash();
  };

  /* ================= boot ================= */

  const load = async () => {
    const src = new URLSearchParams(location.search).get('data');
    if (!src) return window.RECAP_SAMPLE;
    const res = await fetch(src, { headers: { Accept: 'application/json' }, credentials: 'include' });
    return res.json();
  };

  load()
    .then(run)
    .catch(e => {
      console.error(e);
      $('#stage').innerHTML = `<section class="slide"><p class="headline">Couldn't load recap data</p><p class="small">${esc(e.message)}</p></section>`;
    });
})();
