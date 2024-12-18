import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap, Sources, RecapPerf } from './interfaces';
import { onInsert, looseH as h, VNodeKids, VNode, dataIcon } from 'common/snabbdom';
import { formatNumber, loadOpeningLpv } from './ui';
import { shuffle } from 'common/algo';
import { fullName, userFlair, userTitle } from 'common/userLink';
import { spinnerVdom } from 'common/spinner';
import { formatDuration, perfLabel, perfNames } from './util';
import perfIcons from 'common/perfIcons';

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
      r.games.nbs.win && h('p', ['And you won ', h('strong', animateNumber(r.games.nbs.win)), '!']),
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
  return slideTag(
    'moves',
    6000,
  )([
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
  h('a', { attrs: { href: `/@/${o.name}` } }, [userFlair(o) || noFlair(o), userTitle(o), o.name]);

const userFallbackFlair = new Map<string, string>();
const noFlair = (o: LightUser): VNode => {
  let randomFlair =
    userFallbackFlair.get(o.id) ||
    userFallbackFlair
      .set(
        o.id,
        (() =>
          shuffle([
            'activity.lichess-horsey',
            'activity.lichess-hogger',
            'activity.lichess-horsey-yin-yang',
          ])[0])(),
      )
      .get(o.id)!;
  return h('img.uflair.noflair', { attrs: { src: site.asset.flairSrc(randomFlair) } });
};

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

export const openingColor = (os: ByColor<Counted<Opening>>, color: Color): VNode | undefined => {
  const o = os[color];
  if (!o.count) return;
  return slideTag('openings')([
    h('div.lpv.lpv--todo.lpv--moves-bottom.is2d', {
      hook: onInsert(el => loadOpeningLpv(el, color, o.value)),
    }),
    h(
      'div',
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
        h('br'),
        'with ',
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
            r.puzzles.nbs.win &&
              h('p', [
                'You won ',
                h('strong', animateNumber(r.puzzles.nbs.win)),
                ' of them on the first try!',
              ]),
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
    ['pool', 'Pool pairing'],
    ['lobby', 'Lobby custom games'],
  ];
  const best: [string, number][] = all.map(([k, n]) => [n, r.games.sources[k] || 0]);
  best.sort((a, b) => b[1] - a[1]);
  return (
    best[0] &&
    slideTag('sources')([
      h('div.recap--massive', 'Where did you find games?'),
      h(
        'table.recap__data',
        h(
          'tbody',
          best.map(
            ([n, c]) => c > 0 && h('tr', [h('td', n), h('td', [h('strong', animateNumber(c)), ' games'])]),
          ),
        ),
      ),
    ])
  );
};

export const perfs = (r: Recap): VNode => {
  return slideTag('perfs')([
    h('div.recap--massive', 'What time controls and variants did you play?'),
    h(
      'table.recap__data',
      h(
        'tbody',
        r.games.perfs.map(p =>
          h('tr', [h('td', renderPerf(p)), h('td', [h('strong', animateNumber(p.games)), ' games'])]),
        ),
      ),
    ),
  ]);
};

export const malware = (): VNode =>
  slideTag('malware')([
    h('div.recap--massive', [h('strong.animated-pulse', '0'), 'ads and trackers loaded']),
    h('ul', [h('li', "We didn't sell your personal data"), h('li', "We didn't use your device against you")]),
    h(
      'p',
      h('small', [
        'But other websites do, so please ',
        h('a', { attrs: { href: '/ads', target: '_blank' } }, 'be careful.'),
      ]),
    ),
  ]);

export const lichessGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = (r.games.nbs.total * 100) / totalGames;
  const showGamesPercentOfTotal = gamesPercentOfTotal.toFixed(6) + '%';
  return slideTag('lichess-games')([
    h('div.recap--massive', [h('strong', animateNumber(totalGames)), 'games played on Lichess in ', r.year]),
    h('div', [h('p', [h('strong', showGamesPercentOfTotal), ' of them are yours.'])]),
  ]);
};

export const thanks = (): VNode =>
  slideTag('thanks')([
    h('div.recap--massive', 'Thank you for playing on Lichess!'),
    h('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    h('div', "We're glad you're here. Have a great 2025!"),
  ]);

const renderPerf = (perf: RecapPerf): VNode => {
  return h('span', [h('i.text', { attrs: dataIcon(perfIcons[perf.key]) }), perfNames[perf.key] || perf.key]);
};

const stat = (value: string | VNode, label: string): VNode =>
  h('div.stat', [h('div', h('strong', value)), h('div', h('small', label))]);

export const shareable = (r: Recap): VNode =>
  slideTag('shareable')([
    h('div.recap__shareable', [
      h('img.logo', { attrs: { src: site.asset.url('logo/logo-with-name-dark.png') } }),
      h('h2', 'My 2024 Recap'),
      h('div.grid', [
        stat(formatNumber(r.games.nbs.total), 'games played'),
        stat(formatNumber(r.games.moves), 'moves played'),
        stat(formatDuration(r.games.timePlaying, ' and '), 'spent playing'),
        r.games.perfs[0]?.games && stat(renderPerf(r.games.perfs[0]), perfLabel(r.games.perfs[0])),
        r.games.opponents.length && stat(opponentLink(r.games.opponents[0].value), 'most played opponent'),
        stat(formatNumber(r.puzzles.nbs.total), 'puzzles solved'),
      ]),
      h('div.openings', [
        r.games.openings.white.count && stat(r.games.openings.white.value.name, 'as white'),
        r.games.openings.black.count && stat(r.games.openings.black.value.name, 'as black'),
      ]),
    ]),
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
