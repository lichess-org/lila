import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap } from './interfaces';
import { onInsert, looseH as h, VNodeKids, VNode } from 'common/snabbdom';
import { loadOpeningLpv } from './ui';
import { fullName, userFlair, userTitle } from 'common/userLink';
import { spinnerVdom } from 'common/spinner';

const hi = (user: LightUser): VNode => h('h2', ['Hi, ', h('span.recap__user', [...fullName(user)])]);

export const loading = (user: LightUser): VNode =>
  slideTag('await')([hi(user), h('p', 'What have you been up to this year?'), spinnerVdom()]);

export const init = (user: LightUser): VNode =>
  slideTag(
    'init',
    3000,
  )([
    hi(user),
    h('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('h2', 'What a chess year you had!'),
  ]);

export const nbGames = (r: Recap): VNode => {
  return slideTag('games')([
    h('div.recap--massive', [h('strong', animateNumber(r.games.nbs.total)), 'games played']),
    h('div', [h('p', ['And you won ', h('strong', animateNumber(r.games.nbs.win)), '!'])]),
  ]);
};

export const timeSpentPlaying = (r: Recap): VNode => {
  return slideTag('time')([
    h('div.recap--massive', [h('strong', animateTime(r.games.timePlaying)), 'spent playing!']),
    h('div', [h('p', 'Time well wasted, really.')]),
  ]);
};

export const nbMoves = (r: Recap): VNode => {
  return slideTag('moves')([
    h('div.recap--massive', [h('strong', animateNumber(r.games.moves)), 'moves played']),
    h('div', [
      h('p', ["That's ", h('strong', showGrams(r.games.moves * pieceGrams)), ' of wood pushed!']),
      h('p', [h('small', 'Standard pieces weigh about 40g each')]),
    ]),
  ]);
};

export const opponents = (r: Recap): VNode => {
  return slideTag('opponents')([
    h('div.recap--massive', 'Your best chess foes'),
    h(
      'table',
      h(
        'tbody',
        r.games.opponents.map(o =>
          h('tr', [h('td', opponentLink(o.value)), h('td', [animateNumber(o.count), ' games'])]),
        ),
      ),
    ),
  ]);
};

const opponentLink = (o: LightUser): VNode =>
  h('a', { attrs: { href: `/@/${o.name}` } }, [userFlair(o) || noFlair(), userTitle(o), o.name]);

const noFlair = (): VNode =>
  h('img.uflair.noflair', { attrs: { src: site.asset.flairSrc('nature.cat-face') } });

export const firstMoves = (r: Recap, firstMove: Counted<string>): VNode => {
  const percent = Math.round((firstMove.count * 100) / r.games.nbWhite);
  return slideTag('first')([
    h('div.recap--massive', [h('strong.animated-pulse', '1. ' + firstMove.value)]),
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

export const openingColor = (os: ByColor<Counted<Opening>>, color: Color): VNode => {
  const o = os[color];
  return slideTag('openings')([
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

export const malware = (): VNode =>
  slideTag('malware')([
    h('div.recap--massive', [h('strong.animated-pulse', '0'), 'ads and trackers loaded']),
    h('p', "We didn't sell your personal data, and we didn't use your device against you."),
    h(
      'p',
      h('small', [
        'But other websites do, so ',
        h('a', { attrs: { href: '/ads', target: '_blank' } }, 'be careful'),
      ]),
    ),
  ]);

export const lichessGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = (r.games.nbs.total * 100) / totalGames;
  const showGamesPercentOfTotal = gamesPercentOfTotal.toFixed(6) + '%';
  return slideTag('lichess-games')([
    h('div.recap--massive', [
      h('strong', animateNumber(totalGames)),
      'games were played on Lichess in ',
      r.year,
    ]),
    h('div', [h('p', [h('strong', showGamesPercentOfTotal), ' of them are yours.'])]),
  ]);
};

export const bye = (): VNode =>
  slideTag('bye')([
    h('div.recap--massive', 'Thank you for playing on Lichess!'),
    h('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('div', "May your pieces find their way to your opponent's kings."),
  ]);

const slideTag =
  (key: string, millis: number = 5000) =>
  (content: VNodeKids) =>
    h(
      `div.swiper-slide.recap__slide--${key}`,
      {
        attrs: {
          'data-swiper-autoplay': millis,
        },
      },
      content,
    );

const animateNumber = (n: number) => h('span.animated-number', { attrs: { 'data-value': n } }, '0');
const animateTime = (n: number) => h('span.animated-time', { attrs: { 'data-value': n } }, '');

const showGrams = (g: number) =>
  g > 20_000 ? h('span', [animateNumber(g / 1000), ' Kilograms']) : h('span', [animateNumber(g), ' grams']);
