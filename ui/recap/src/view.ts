import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap } from './interfaces';
import { h, type VNode } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { loadOpeningLpv } from './ui';
import { fullName } from 'common/userLink';
import { spinnerVdom } from 'common/spinner';
import { showDuration } from './util';

export function awaiter(user: LightUser): VNode {
  return h('div#recap-swiper.swiper.swiper-initialized', [
    h('div.swiper-wrapper', [
      h(slideTag('await'), [hi(user), h('p', 'What have you been up to this year?'), spinnerVdom()]),
    ]),
  ]);
}

export function view(r: Recap, user: LightUser): VNode {
  return h('div#recap-swiper.swiper', [
    h('div.swiper-wrapper', [
      init(user),
      nbGames(r),
      timeSpentPlaying(r),
      nbMoves(r),
      r.games.firstMoves[0] && firstMoves(r, r.games.firstMoves[0]),
      openingColor(r.games.openings, 'white'),
      openingColor(r.games.openings, 'black'),
      malware(),
      bye(),
    ]),
    h('div.swiper-button-next'),
    h('div.swiper-button-prev'),
    h('div.swiper-pagination'),
    h('div.autoplay-progress', [
      h('svg', { attrs: { viewBox: '0 0 48 48' } }, [h('circle', { attrs: { cx: 24, cy: 24, r: 20 } })]),
      h('span'),
    ]),
  ]);
}

const hi = (user: LightUser): VNode => h('h2', ['Hi, ', h('span.recap__user', [...fullName(user)])]);

const init = (user: LightUser): VNode =>
  h(slideTag('init'), [
    hi(user),
    h('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('h2', 'What a chess year you had!'),
  ]);

const nbGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = (r.games.nb.nb * 100) / totalGames;
  const showGamesPercentOfTotal = gamesPercentOfTotal.toFixed(6) + '%';
  return h(slideTag('games'), [
    h('div.recap--massive', [h('strong', animateNumber(r.games.nb.nb)), 'games played']),
    h('div', [h('p', [h('strong', showGamesPercentOfTotal), ' of all games played this year!'])]),
  ]);
};

const timeSpentPlaying = (r: Recap): VNode => {
  return h(slideTag('time'), [
    h('div.recap--massive', [h('strong', showDuration(r.games.timePlaying)), 'spent playing!']),
    h('div', [h('p', 'Time well wasted, really.')]),
  ]);
};

const nbMoves = (r: Recap): VNode => {
  return h(slideTag('moves'), [
    h('div.recap--massive', [h('strong', animateNumber(r.games.moves)), 'moves played']),
    h('div', [
      h('p', ["That's ", h('strong', showGrams(r.games.moves * pieceGrams)), ' of wood pushed!']),
      h('p', [h('small', 'Standard pieces weigh about 40g each')]),
    ]),
  ]);
};

const firstMoves = (r: Recap, firstMove: Counted<string>): VNode => {
  const percent = Math.round((firstMove.count * 100) / r.games.nbWhite);
  return h(slideTag('first'), [
    h('div.recap--massive', [h('strong', '1. ' + firstMove.value)]),
    h('div', [
      h('p', [
        'is how you started ',
        h('strong', animateNumber(firstMove.count)),
        ' (',
        animateNumber(percent),
        '%) of your games as white',
      ]),
    ]),
  ]);
};

const openingColor = (os: ByColor<Counted<Opening>>, color: Color): VNode => {
  const o = os[color];
  return h(slideTag('openings'), [
    h('div.lpv.lpv--todo.lpv--moves-bottom.is2d', {
      hook: onInsert(el => loadOpeningLpv(el, color, o.value)),
    }),
    h(
      'div.recap--big',
      h(
        'a',
        {
          attrs: { href: `/opening/${o.value.key}`, target: '_blank' },
        },
        o.value.name,
      ),
    ),
    h('div', [
      h('p', [
        'Your most played opening as ',
        color,
        ', with ',
        h('strong', animateNumber(o.count)),
        ' games.',
      ]),
    ]),
  ]);
};

const malware = () =>
  h(slideTag('malware'), [
    h('div.recap--massive', [h('strong', '0'), 'ads and trackers loaded']),
    h('p', "We didn't sell your personal data, and we didn't use your device against you."),
    h(
      'p',
      h('small', [
        'But other websites do, so ',
        h('a', { attrs: { href: '/ads', target: '_blank' } }, 'be careful'),
      ]),
    ),
  ]);

const bye = () =>
  h(slideTag('bye'), [
    h('div.recap--massive', 'Thank you for playing on Lichess!'),
    h('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('div', "May your pieces find their way to your opponent's kings."),
  ]);

const slideTag = (key: string) => `div.swiper-slide.recap__slide--${key}`;

const animateNumber = (n: number) => h('span.animated-number', { attrs: { 'data-value': n } }, '0');

const showGrams = (g: number) =>
  g > 20_000 ? h('span', [animateNumber(g / 1000), ' Kilograms']) : h('span', [animateNumber(g), ' grams']);
