import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap } from './interfaces';
import { h, type VNode } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { loadOpeningLpv } from './ui';
import { fullName } from 'common/userLink';

export default function view(r: Recap, user: LightUser): VNode {
  return h('div#recap-swiper.swiper', [
    h('div.swiper-wrapper', [
      init(user),
      nbGames(r),
      timeSpentPlaying(r),
      nbMoves(r),
      firstMove(r),
      openingColor(r.games.openings, 'white'),
      openingColor(r.games.openings, 'black'),
    ]),
    h('div.swiper-pagination'),
  ]);
}

const init = (user: LightUser): VNode =>
  h(slideTag('init'), [
    h('h2', ['Hi, ', h('span.recap__user', [...fullName(user)])]),
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

const firstMove = (r: Recap): VNode => {
  const percent = Math.round((r.games.firstMove.count * 100) / r.games.nb.nb);
  return h(slideTag('first'), [
    h('div.recap--massive', [h('strong', '1. ' + r.games.firstMove.value)]),
    h('div', [
      h('p', [
        'is how you started ',
        h('strong', animateNumber(r.games.firstMove.count)),
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

const slideTag = (key: string) => `div.swiper-slide.recap__slide--${key}`;

const animateNumber = (n: number) => h('span.animated-number', { attrs: { 'data-value': n } }, '0');

const showGrams = (g: number) =>
  g > 20_000 ? h('span', [animateNumber(g / 1000), ' Kilograms']) : h('span', [animateNumber(g), ' grams']);

function showDuration(seconds: number): string {
  const days = Math.floor(seconds / (24 * 3600));
  seconds %= 24 * 3600;
  const hours = Math.floor(seconds / 3600);
  seconds %= 3600;
  const minutes = Math.floor(seconds / 60);

  let result = '';
  if (days > 0) {
    result += `${days} days`;
  }
  if (hours > 0) {
    if (result) result += ' and ';
    result += `${hours} hours`;
  }
  if (days == 0) {
    if (result) result += ' and ';
    result += `and ${minutes} minutes`;
  }

  return result;
}
