import { shuffle } from 'lib/algo';
import { perfName } from 'lib/game/perf';
import perfIcons from 'lib/game/perfIcons';
import { percentFormat } from 'lib/i18n';

import { makeBoard, parsePgnMoves } from './board';
import { grandPianoGrams, lotrSeconds, ounceGrams, pieceGrams, poundGrams, totalGames } from './constants';
import { renderDonate } from './donate';
import {
  countSpan,
  easeInOut,
  easeOutExpo,
  esc,
  flairOf,
  fmt,
  pieceSrc,
  pluralLabel,
  raw,
  type Raw,
  setText,
  trHtml,
  userLabel,
  words,
} from './html';
import type { Recap, SlideCtx, SlideDef, Sources } from './interfaces';
import { countAll, countUp, typeText } from './scope';
import { formatDuration, perfLabel } from './util';
import { galaxy, layoutBubbles, pilePieces } from './visuals';

const logo = () => site.asset.url('logo/lichess-white.svg');

const perfIcon = (key: Perf) => `<i class="perf-icon" data-icon="${perfIcons[key]}"></i>`;

const sourceNames = (): Record<keyof Sources, string> => ({
  friend: i18n.preferences.notifyChallenge,
  ai: i18n.site.computer,
  arena: i18n.arena.arenaTournaments,
  swiss: i18n.swiss.swissTournaments,
  simul: i18n.site.simultaneousExhibitions,
  pool: i18n.site.quickPairing,
  lobby: i18n.site.lobby,
});

export const loading = (user: LightUser): string =>
  `<section class="slide">
    <p class="kicker">${trHtml(i18n.recap.hiUser.asArray(raw(userLabel(user))))}</p>
    <p class="headline">${words(i18n.recap.awaitQuestion)}</p>
    <div class="loader"><i></i><i></i><i></i></div>
  </section>`;

export function buildSlides(ctx: SlideCtx): SlideDef[] {
  const { opts, bg } = ctx;
  const R: Recap = opts.recap!;
  const U = opts.user;
  const G = R.games;

  const intro: SlideDef = {
    key: 'intro',
    theme: ['#7c3aed', '#06b6d4', '#f43f5e'],
    dur: 6000,
    render(el, sc) {
      const kinds = ['wK', 'bQ', 'wN', 'bR', 'wB', 'bP', 'wQ', 'bN', 'wR', 'bB', 'wP', 'bK'] as const;
      el.innerHTML = `
        <div class="intro__orbit"><div class="intro__ring">
          ${kinds.map(k => `<img src="${pieceSrc(k[0] as 'w' | 'b', k[1])}" alt="">`).join('')}
        </div></div>
        <img class="intro__logo reveal" src="${logo()}" alt="Lichess">
        <p class="kicker reveal" style="--d:.25s">${trHtml(i18n.recap.hiUser.asArray(raw(userLabel(U))))}</p>
        <h1 class="intro__year">${String(R.year)
          .split('')
          .map((d, i) => `<span style="--d:${0.5 + i * 0.12}s">${esc(d)}</span>`)
          .join('')}</h1>
        <p class="headline">${words(i18n.recap.initTitle, 1.3)}</p>`;
      const imgs = [...el.querySelectorAll<HTMLElement>('.intro__ring img')];
      const front = imgs.map(() => false);
      // transform and opacity only, so the browser can move the pieces without repainting them
      sc.loop(t => {
        const { W, H } = bg.size();
        const rx = Math.min(W * 0.44, 520);
        const ry = Math.min(H * 0.3, 260);
        imgs.forEach((img, i) => {
          const a = t / 4200 + (i / imgs.length) * Math.PI * 2;
          const z = Math.sin(a); // -1 back, 1 front
          const s = 0.55 + (z + 1) * 0.4;
          img.style.transform = `translate3d(${Math.cos(a) * rx}px, ${z * ry}px, 0) scale(${s}) rotate(${Math.cos(a) * 18}deg)`;
          img.style.opacity = String(0.35 + (z + 1) * 0.3);
          if (front[i] !== z > 0) {
            front[i] = z > 0;
            img.style.zIndex = front[i] ? '2' : '-1';
          }
        });
      });
      sc.after(1600, () => sc.onDispose(bg.fireworks(6, 2200)));
    },
  };

  const noGames: SlideDef | false = !G.nbs.total && {
    key: 'no-games',
    theme: ['#475569', '#06b6d4', '#7c3aed'],
    render(el) {
      el.innerHTML = `<p class="headline">${words(i18n.recap.noGamesText)}</p>`;
    },
  };
  const games: SlideDef | false = G.nbs.total > 0 && {
    key: 'games',
    theme: ['#2563eb', '#22d3ee', '#a855f7'],
    dur: 5000,
    render(el, sc) {
      const C = 2 * Math.PI * 88;
      const winRate = G.nbs.win / G.nbs.total;
      el.innerHTML = `
          <p class="mega num" data-count="${G.nbs.total}">0</p>
          <p class="headline reveal" style="--d:.3s">${esc(pluralLabel(i18n.site.nbGames, G.nbs.total))}</p>
          <div class="ring reveal" style="--d:.6s">
            <svg viewBox="0 0 200 200">
              <defs><linearGradient id="ring-grad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="var(--c2)"/><stop offset="1" stop-color="var(--c3)"/>
              </linearGradient></defs>
              <circle class="track" cx="100" cy="100" r="88"/>
              <circle class="bar" cx="100" cy="100" r="88" stroke-dasharray="${C}" stroke-dashoffset="${C}"/>
            </svg>
            <div class="ring__inner"><b class="num ring__rate">0%</b><small class="small">${esc(i18n.site.winRate)}</small></div>
          </div>
          ${G.nbs.win ? `<p class="lead reveal" style="--d:1.2s">${trHtml(i18n.recap.gamesYouWon.asArray(countSpan(G.nbs.win, 1.2)))}</p>` : ''}`;
      countAll(sc, el);
      // percentFormat builds a new Intl formatter on each call, too slow for every frame
      const percents = new Map<number, string>();
      const percent = (v: number) => {
        const p = Math.round(v * 100);
        if (!percents.has(p)) percents.set(p, percentFormat(p / 100, 0));
        return percents.get(p)!;
      };
      sc.after(700, () => {
        el.querySelector<SVGElement>('.bar')!.style.strokeDashoffset = String(C * (1 - winRate));
        countUp(sc, el.querySelector('.ring__rate'), winRate, { dur: 2400, format: percent });
      });
      sc.after(3000, () => {
        const r = el.querySelector('.ring')!.getBoundingClientRect();
        bg.burst(r.left + r.width / 2, r.top + r.height / 2, 120);
      });
    },
  };

  const time: SlideDef | false = G.nbs.total > 0 && {
    key: 'time',
    theme: ['#0ea5e9', '#a3e635', '#14b8a6'],
    dur: 7000,
    render(el, sc) {
      const s = G.timePlaying;
      const days = s / 86400;
      const lotr = Math.round(s / lotrSeconds);
      const verdict =
        days > 10
          ? i18n.recap.timeDedication
          : days > 5
            ? i18n.recap.timeEveryMinuteCounted
            : i18n.recap.timeWellSpent;
      el.innerHTML = `
          <p class="kicker reveal">${esc(i18n.recap.timeSpentPlaying)}</p>
          <div class="clock reveal" style="--d:.1s">
            <div class="clock__time"><span>0</span><sep>:</sep><span>00</span><sep>:</sep><span>00</span></div>
            <div class="clock__bar"></div>
          </div>
          <p class="lead reveal" style="--d:1.6s">${esc(verdict)}</p>
          ${lotr >= 1 ? `<p class="small reveal" style="--d:2.2s">${trHtml(i18n.recap.timeLordOfTheRings.asArray(lotr, raw(`<span class="hl">${fmt(lotr)}</span>`)))} 🍿</p>` : ''}
          <p class="small reveal" style="--d:2.8s">${esc(i18n.recap.timeHowManyMoves)}</p>`;
      // a game clock running up to the time played, in hours, minutes and seconds
      const [hh, mm, ss] = el.querySelectorAll<HTMLElement>('.clock__time span');
      const pad = (n: number) => String(n).padStart(2, '0');
      sc.after(300, () =>
        sc.loop(t => {
          const k = easeOutExpo(Math.min(1, t / 3200));
          const v = Math.floor(s * k);
          setText(hh, String(Math.floor(v / 3600)));
          setText(mm, pad(Math.floor((v % 3600) / 60)));
          setText(ss, pad(v % 60));
          return k < 1;
        }),
      );
    },
  };

  const moves: SlideDef | false = G.nbs.total > 0 && {
    key: 'moves',
    theme: ['#d97706', '#f59e0b', '#ef4444'],
    dur: 7500,
    top: true,
    render(el, sc) {
      const grams = G.moves * pieceGrams;
      const pianos = Math.round(grams / grandPianoGrams);
      const weigh = (plural: I18nPlural, n: number) => plural.asArray(n, countSpan(n, 1.4));
      const weight = opts.imperial
        ? grams >= poundGrams
          ? weigh(i18n.recap.nbPounds, Math.round(grams / poundGrams))
          : weigh(i18n.recap.nbOunces, Math.round(grams / ounceGrams))
        : grams > 20_000
          ? weigh(i18n.recap.nbKilograms, Math.round(grams / 1000))
          : weigh(i18n.recap.nbGrams, grams);
      el.innerHTML = `
          <canvas class="fill"></canvas>
          <p class="mega num" data-count="${G.moves}">0</p>
          <p class="headline reveal" style="--d:.3s">${esc(pluralLabel(i18n.recap.nbMoves, G.moves))}</p>
          <p class="lead reveal" style="--d:1.4s">${trHtml(i18n.recap.movesOfWoodPushed.asArray(raw(`<strong>${trHtml(weight)}</strong>`)))}</p>
          ${pianos >= 1 ? `<p class="small reveal" style="--d:2.2s">${trHtml(i18n.recap.movesGrandPianos.asArray(pianos, raw(`<span class="hl">${fmt(pianos)}</span>`)))} 🎹</p>` : ''}
          <p class="small reveal" style="--d:2.6s">${esc(opts.imperial ? i18n.recap.movesStandardPiecesWeightImperial : i18n.recap.movesStandardPiecesWeight)}</p>`;
      countAll(sc, el, { dur: 2600 });
      pilePieces(sc, el.querySelector('canvas')!, Math.min(360, 80 + Math.sqrt(G.moves) * 0.9));
    },
  };

  const perfs: SlideDef | false = G.perfs.length > 0 && {
    key: 'perfs',
    theme: ['#db2777', '#f472b6', '#8b5cf6'],
    render(el, sc) {
      const max = Math.max(...G.perfs.map(p => p.games));
      el.innerHTML = `
            <p class="headline">${words(i18n.recap.perfsTitle)}</p>
            <div class="bars">${G.perfs
              .slice(0, 8)
              .map((p, i) => {
                const d = 0.4 + i * 0.14;
                return `<div class="bars__row" style="--d:${d - 0.1}s">
                  ${perfIcon(p.key)}
                  <div class="bars__track">
                    <div class="bars__fill" data-w="${p.games / max}" style="--d:${d}s"></div>
                    <div class="bars__label"><span>${esc(perfName(p.key))}</span><span class="num" data-count="${p.games}" data-delay="${d}">0</span></div>
                  </div>
                </div>`;
              })
              .join('')}</div>`;
      countAll(sc, el, { dur: 1800 });
      sc.after(50, () =>
        el
          .querySelectorAll<HTMLElement>('.bars__fill')
          .forEach(b => (b.style.transform = `scaleX(${Math.max(0.04, parseFloat(b.dataset.w!))})`)),
      );
    },
  };

  const names = sourceNames();
  const sources = (Object.entries(G.sources) as [keyof Sources, number][])
    .filter(([, c]) => c > 0)
    .sort((a, b) => b[1] - a[1])
    .map(([k, c]): [string, number] => [names[k] ?? k, c]);
  const sourcesSlide: SlideDef | false = sources.length > 0 && {
    key: 'sources',
    theme: ['#059669', '#34d399', '#0ea5e9'],
    render(el) {
      el.innerHTML = `<p class="headline">${words(i18n.recap.sourcesTitle)}</p><div class="bubbles"></div>`;
      layoutBubbles(el.querySelector('.bubbles')!, sources);
    },
  };

  const opponents: SlideDef | false = G.opponents.length > 0 && {
    key: 'opponents',
    theme: ['#dc2626', '#f97316', '#7c3aed'],
    render(el, sc) {
      const [top, ...rest] = G.opponents;
      el.innerHTML = `
            <p class="kicker reveal">${esc(i18n.recap.greatestChessFoes)}</p>
            <div class="versus">
              <div class="fighter fighter--l"><div class="avatar avatar--you"><span>${esc(U.name[0].toUpperCase())}</span></div>${userLabel(U)}</div>
              <div class="vs">VS</div>
              <div class="fighter fighter--r">
                <div class="avatar"><img src="${site.asset.flairSrc(flairOf(top.value))}" alt=""></div>${userLabel(top.value)}
              </div>
            </div>
            <p class="lead reveal" style="--d:1.3s">${trHtml(i18n.site.nbGames.asArray(top.count, countSpan(top.count, 1.3)))}</p>
            ${
              rest.length
                ? `<ol class="rivals">${rest
                    .map(
                      (o, i) => `<li style="--d:${1.9 + i * 0.15}s">
                      <span class="rivals__name"><span class="rank">${i + 2}</span><img src="${site.asset.flairSrc(flairOf(o.value))}" alt="">${userLabel(o.value)}</span>
                      <span class="num">${esc(i18n.site.nbGames(o.count, fmt(o.count)))}</span></li>`,
                    )
                    .join('')}</ol>`
                : ''
            }`;
      countAll(sc, el, { dur: 1500 });
      sc.after(950, () => {
        const r = el.querySelector('.vs')!.getBoundingClientRect();
        bg.burst(r.left + r.width / 2, r.top + r.height / 2, 80, ['#f97316', '#fde047', '#ef4444']);
        bg.warp(0.6);
      });
    },
  };

  // first move and favourite openings, on one slide with a board per colour
  const first = G.nbWhite > 0 ? G.firstMoves[0] : undefined;
  const favouriteOpenings = (['white', 'black'] as const).filter(c => G.openings[c]?.count);
  const openings: SlideDef | false = (!!first || favouriteOpenings.length > 0) && {
    key: 'openings',
    theme: ['#4f46e5', '#818cf8', '#22d3ee'],
    dur: 9500,
    render(el, sc) {
      const stats =
        first &&
        raw(`${countSpan(first.count, 0.6).html} (${esc(percentFormat(first.count / G.nbWhite, 0))})`);
      el.innerHTML = `
        ${
          first && stats
            ? `<p class="kicker reveal">${esc(i18n.recap.firstMoveTitle)}</p>
              <p class="mega mega--sm reveal">1. ${esc(first.value)}</p>
              <p class="lead reveal" style="--d:.6s">${trHtml(i18n.recap.firstMoveStats.asArray(stats))}</p>`
            : ''
        }
        <div class="openings"></div>`;
      const grid = el.querySelector('.openings')!;
      favouriteOpenings.forEach((color, i) => {
        const o = G.openings[color];
        const delay = 1 + i * 0.25;
        const board = makeBoard(color);
        const card = document.createElement('div');
        card.className = 'opening reveal';
        card.style.setProperty('--d', `${delay}s`);
        card.appendChild(board.el);
        card.insertAdjacentHTML(
          'beforeend',
          `<p class="opening__name typed"></p>
          <small>${esc(color === 'white' ? i18n.site.asWhite : i18n.site.asBlack)} · ${trHtml(
            i18n.site.nbGames.asArray(o.count, countSpan(o.count, delay + 0.4, 'b')),
          )}</small>`,
        );
        grid.appendChild(card);
        typeText(sc, card.querySelector<HTMLElement>('.opening__name')!, o.value.name, delay * 1000 + 300);
        const moves = parsePgnMoves(o.value.pgn);
        const step = Math.min(700, 5600 / Math.max(1, moves.length));
        moves.forEach((san, m) => sc.after(delay * 1000 + 900 + m * step, () => board.play(san)));
      });
      countAll(sc, el, { dur: 1600 });
    },
  };

  const P = R.puzzles;
  // skipped for players who solved no puzzles
  const puzzles: SlideDef | false = P.nbs.total > 0 && {
    key: 'puzzles',
    theme: ['#16a34a', '#4ade80', '#facc15'],
    dur: 6500,
    render(el, sc) {
      const cells = Math.min(P.nbs.total, 400);
      const cols = cells > 200 ? 25 : cells > 60 ? 15 : 10;
      const wins = Math.round((P.nbs.win / P.nbs.total) * cells);
      el.innerHTML = `
        <p class="mega mega--sm num" data-count="${P.nbs.total}">0</p>
        <p class="headline reveal" style="--d:.2s">${esc(pluralLabel(i18n.site.nbPuzzles, P.nbs.total))}</p>
        <div class="mosaic" style="--cols:${cols}">${'<i></i>'.repeat(cells)}</div>
        <div class="legend reveal" style="--d:.6s">
          <span><i class="w"></i>${esc(i18n.recap.puzzlesFirstTry)}</span>
          <span><i class="l"></i>${esc(i18n.recap.puzzlesAnotherGo)}</span>
        </div>
        ${P.nbs.win ? `<p class="lead reveal" style="--d:2.4s">${trHtml(i18n.recap.puzzlesYouSolvedOnFirstTry.asArray(countSpan(P.nbs.win, 2.4)))}</p>` : ''}
        ${
          P.votes.nb
            ? `<p class="small reveal" style="--d:3s">${esc(i18n.recap.puzzlesThanksVoting(P.votes.nb, fmt(P.votes.nb)))}
              ${P.votes.themes ? esc(i18n.recap.puzzlesHelpedTagging(fmt(P.votes.themes))) : ''} 💚</p>`
            : ''
        }`;
      countAll(sc, el, { dur: 2400 });
      const nodes = [...el.querySelectorAll('.mosaic i')];
      const order = shuffle(nodes.map((_, i) => i));
      const kinds = shuffle(order.map((_, i) => (i < wins ? 'w' : 'l')));
      let done = 0;
      sc.after(400, () =>
        sc.loop(t => {
          const target = Math.floor(easeInOut(Math.min(1, t / 2200)) * cells);
          for (; done < target; done++) nodes[order[done]].className = kinds[done];
          return done < cells;
        }),
      );
    },
  };

  const lichessGames: SlideDef | false = G.nbs.total > 0 && {
    key: 'lichess-games',
    theme: ['#312e81', '#c084fc', '#38bdf8'],
    dur: 7500,
    render(el, sc) {
      const mark = raw('');
      const playedIn = i18n.recap.lichessGamesPlayedIn
        .asArray<Raw | string>(mark, String(R.year))
        .filter(p => p !== mark)
        .join('')
        .trim();
      el.innerHTML = `
          <div class="galaxy"></div>
          <p class="mega mega--sm num" data-count="${totalGames}">0</p>
          <p class="headline">${words(playedIn, 0.3)}</p>
          <p class="lead reveal" style="--d:2.4s">${trHtml(i18n.recap.lichessGamesOfThemYours.asArray(raw(`<strong>${esc(percentFormat(G.nbs.total / totalGames, 6))}</strong>`)))} ✨</p>
          <p class="small reveal" style="--d:3.2s">${esc(i18n.recap.gamesNextQuestion)}</p>`;
      countAll(sc, el, { dur: 2600 });
      galaxy(el.querySelector('.galaxy')!, i18n.recap.lichessGamesYou);
    },
  };

  const malware: SlideDef = {
    key: 'malware',
    theme: ['#0f766e', '#2dd4bf', '#84cc16'],
    render(el) {
      const blocked = [
        i18n.recap.malwareAds,
        i18n.recap.malwareTrackers,
        i18n.recap.malwareDataBrokers,
        i18n.recap.malwarePopups,
        i18n.recap.malwarePaywalls,
      ];
      el.innerHTML = `
        <div class="shield reveal">
          <svg viewBox="0 0 100 110"><defs><linearGradient id="sg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--c2)"/><stop offset="1" stop-color="var(--c1)"/></linearGradient></defs>
            <path d="M50 4 L92 18 V52 C92 80 72 98 50 106 C28 98 8 80 8 52 V18 Z" fill="url(#sg)" fill-opacity=".25" stroke="url(#sg)" stroke-width="4"/></svg>
          <div class="shield__zero">0</div><div class="shield__scan"></div>
        </div>
        <p class="headline">${words(i18n.recap.malwareNoneLoaded('0'), 0.3)}</p>
        <div class="blocked">${blocked.map((b, i) => `<span style="--d:${0.6 + i * 0.12}s">${esc(b)}</span>`).join('')}</div>
        <ul class="checks">
          <li style="--d:2s">${esc(i18n.recap.malwareNoSell)}</li>
          <li style="--d:2.3s">${esc(i18n.recap.malwareNoAbuse)}</li>
        </ul>`;
    },
  };

  const share: SlideDef = {
    key: 'share',
    theme: ['#7c3aed', '#06b6d4', '#f43f5e'],
    dur: 9000,
    render(el, sc) {
      const topPerf = G.perfs[0];
      const stat = (value: string, label: string, wide = false) =>
        `<div class="card__stat${wide ? ' card__stat--wide' : ''}"><b>${value}</b><small>${esc(label)}</small></div>`;
      const countStat = (n: number, plural: I18nPlural) => stat(fmt(n), pluralLabel(plural, n));
      el.innerHTML = `
        <div class="card">
          <div class="card__head"><img src="${logo()}" alt="">
            <div><h3>${esc(i18n.recap.shareableTitleLichess(R.year))}</h3><small>${userLabel(U)} · lichess.org</small></div></div>
          <div class="card__grid">
            ${countStat(G.nbs.total, i18n.site.nbGames)}
            ${countStat(G.moves, i18n.recap.nbMovesPlayed)}
            ${stat(esc(formatDuration(G.timePlaying)), i18n.recap.shareableSpentPlaying, true)}
            ${topPerf?.games ? stat(`${perfIcon(topPerf.key)} ${esc(perfName(topPerf.key))}`, perfLabel(topPerf)) : ''}
            ${G.opponents[0] ? stat(userLabel(G.opponents[0].value), i18n.recap.shareableMostPlayedOpponent) : ''}
            ${P.nbs.total ? countStat(P.nbs.total, i18n.recap.shareableNbPuzzlesSolved) : ''}
            ${G.firstMoves[0] ? stat('1. ' + esc(G.firstMoves[0].value), i18n.recap.shareableFavouriteFirstMove) : ''}
            ${(['white', 'black'] as const)
              .filter(c => G.openings[c]?.count)
              .map(c =>
                stat(
                  esc(G.openings[c].value.name),
                  c === 'white' ? i18n.site.asWhite : i18n.site.asBlack,
                  true,
                ),
              )
              .join('')}
          </div>
        </div>`;
      const card = el.querySelector<HTMLElement>('.card')!;
      sc.after(600, () => {
        const r = card.getBoundingClientRect();
        bg.burst(r.left + r.width / 2, r.top + r.height / 3, 140);
      });
    },
  };

  const donate: SlideDef = {
    key: 'donate',
    theme: ['#14b8a6', '#fbbf24', '#6366f1'],
    dur: Infinity,
    top: true,
    render: (el, sc) => renderDonate(el, sc, ctx),
  };

  return [
    intro,
    noGames,
    games,
    lichessGames,
    time,
    moves,
    perfs,
    sourcesSlide,
    opponents,
    openings,
    puzzles,
    malware,
    share,
    donate,
  ].filter((s): s is SlideDef => !!s);
}
