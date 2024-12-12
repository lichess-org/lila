import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap, Sources } from './interfaces';
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

export const noGames = (): VNode =>
  slideTag('no-games')([
    h('div.recap--massive', 'You did not play any games this year.'),
    h('div', h('p', h('a', { attrs: { href: '/', target: '_blank' } }, 'Wanna play now?'))),
  ]);

export const nbGames = (r: Recap): VNode => {
  return slideTag('games')([
    h('div.recap--massive', [h('strong', animateNumber(r.games.nbs.total)), 'games played']),
    h('div', [
      h('p', ['And you won ', h('strong', animateNumber(r.games.nbs.win)), '!']),
      h('p', 'What did it take to get there?'),
    ]),
  ]);
};

export const timeSpentPlaying = (r: Recap): VNode => {
  const s = r.games.timePlaying;
  const days = s / 60 / 60 / 24;
  return slideTag('time')([
    h('div.recap--massive', [h('strong', animateTime(s)), 'spent playing!']),
    h('div', [
      h(
        'p',
        days > 10
          ? 'That is way too much chess.'
          : days > 5
            ? 'That is a lot of chess.'
            : 'That seems like a reasonable amount of chess.',
      ),
      h('p', 'How many moves did you play in all that time?'),
    ]),
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
      'table.recap__data',
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

export const puzzles = (r: Recap): VNode => {
  return slideTag('puzzles')(
    r.puzzles.nbs.total
      ? [
          h('div.recap--massive', [h('strong', animateNumber(r.puzzles.nbs.total)), 'puzzles solved']),
          h('div', [
            h('p', ['You won ', h('strong', animateNumber(r.puzzles.nbs.win)), ' of them on the first try!']),
            r.puzzles.votes.nb
              ? h('p', [
                  'Thank you for voting on ',
                  h('strong', animateNumber(r.puzzles.votes.nb)),
                  ' puzzles.',
                ])
              : null,
            r.puzzles.votes.themes
              ? h('p', [
                  'You also helped tagging ',
                  h('strong', animateNumber(r.puzzles.votes.themes)),
                  ' of them.',
                ])
              : null,
          ]),
        ]
      : [
          h('div.recap--massive', 'You did not solve any puzzles this year.'),
          h('div', h('p', h('a', { attrs: { href: '/training', target: '_blank' } }, 'Wanna try some now?'))),
        ],
  );
};

export const sources = (r: Recap): VNode => {
  const all: [keyof Sources, string][] = [
    ['friend', 'Challenges'],
    ['ai', 'Computer'],
    ['arena', 'Arena tournaments'],
    ['swiss', 'Swiss tournaments'],
    ['simul', 'Simuls'],
    ['pool', 'Lobby pairing'],
  ];
  const best: [string, number][] = all.map(([k, n]) => [n, r.games.sources[k] || 0]);
  best.sort((a, b) => b[1] - a[1]);
  return (
    best[0] &&
    slideTag('sources')([
      h('div.recap--massive', 'Where did you play?'),
      h(
        'table.recap__data',
        h(
          'tbody',
          best.map(([n, c]) => c > 0 && h('tr', [h('td', n), h('td', [animateNumber(c), ' games'])])),
        ),
      ),
    ])
  );
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
    h('div', "May your pieces find their way to your opponents' kings."),
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
