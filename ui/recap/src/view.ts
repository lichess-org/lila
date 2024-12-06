import { pieceGrams, totalGames } from './constants';
import type { Recap } from './interfaces';
import { h, type VNode } from 'snabbdom';

export default function view(r: Recap): VNode {
  return h('div#recap-swiper.swiper', [
    h('div.swiper-wrapper', [init(), nbGames(r), timeSpentPlaying(r), nbMoves(r)]),
    h('div.swiper-pagination'),
  ]);
}

const init = (): VNode =>
  h(slideTag('init'), [
    h('img', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('h2', 'What a chess year you had!'),
  ]);

const nbGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = (r.games.nb.nb * 100) / totalGames;
  const showGamesPercentOfTotal = gamesPercentOfTotal.toFixed(6) + '%';
  return h(slideTag('games'), [
    h('strong.recap--massive', [h('strong', animateNumber(r.games.nb.nb)), 'games played']),
    h('div', [h('p', [h('strong', showGamesPercentOfTotal), ' of all games played this year!'])]),
  ]);
};

const timeSpentPlaying = (r: Recap): VNode => {
  return h(slideTag('time'), [
    h('strong.recap--massive', [h('strong', showDuration(r.games.timePlaying)), 'spent playing!']),
    h('div', [h('p', 'Time well wasted, really.')]),
  ]);
};

const nbMoves = (r: Recap): VNode => {
  return h(slideTag('moves'), [
    h('strong.recap--massive', [h('strong', animateNumber(r.games.moves)), 'moves played']),
    h('div', [
      h('p', ["That's ", h('strong', showGrams(r.games.moves * pieceGrams)), ' of wood pushed!']),
      h('p', [h('small', 'Standard pieces weight about 40g each')]),
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
