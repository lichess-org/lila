import { pieceGrams, totalGames } from './constants';
import type { ByColor, Counted, Opening, Recap, Sources, RecapPerf } from './interfaces';
import { onInsert, hl, LooseVNodes, VNode, dataIcon } from 'lib/snabbdom';
import { formatNumber, loadOpeningLpv } from './ui';
import { shuffle } from 'lib/algo';
import { fullName, userFlair, userTitle } from 'lib/view/userLink';
import { spinnerVdom } from 'lib/view/controls';
import { formatDuration, perfLabel, perfNames } from './util';
import perfIcons from 'lib/game/perfIcons';

const hi = (user: LightUser): VNode => hl('h2', ['Hi, ', hl('span.recap__user', fullName(user))]);

export const loading = (user: LightUser): VNode =>
  slideTag('await')([hi(user), hl('p', 'What have you been up to this year?'), spinnerVdom()]);

export const init = (user: LightUser): VNode =>
  slideTag(
    'init',
    3000,
  )([
    hi(user),
    hl('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    hl('h2', 'What a chess year you had!'),
  ]);

export const noGames = (): VNode =>
  slideTag('no-games')([
    hl('div.recap--massive', 'You did not play any games this year.'),
    hl('div', hl('p', hl('a', { attrs: { href: '/', target: '_blank' } }, 'Wanna play now?'))),
  ]);

export const nbGames = (r: Recap): VNode => {
  return slideTag('games')([
    hl('div.recap--massive', [hl('strong', animateNumber(r.games.nbs.total)), 'games played']),
    hl('div', [
      r.games.nbs.win && hl('p', ['And you won ', hl('strong', animateNumber(r.games.nbs.win)), '!']),
      hl('p', 'What did it take to get there?'),
    ]),
  ]);
};

export const timeSpentPlaying = (r: Recap): VNode => {
  const s = r.games.timePlaying;
  const days = s / 60 / 60 / 24;
  return slideTag('time')([
    hl('div.recap--massive', [hl('strong', animateTime(s)), 'spent playing!']),
    hl('div', [
      hl(
        'p',
        days > 10
          ? 'That is way too much chess.'
          : days > 5
            ? 'That is a lot of chess.'
            : 'That seems like a reasonable amount of chess.',
      ),
      hl('p', 'How many moves did you play in all that time?'),
    ]),
  ]);
};

export const nbMoves = (r: Recap): VNode => {
  return slideTag(
    'moves',
    6000,
  )([
    hl('div.recap--massive', [hl('strong', animateNumber(r.games.moves)), 'moves played']),
    hl('div', [
      hl('p', ["That's ", hl('strong', showGrams(r.games.moves * pieceGrams)), ' of wood pushed!']),
      hl('p', [hl('small', 'Standard pieces weigh about 40g each')]),
    ]),
  ]);
};

export const opponents = (r: Recap): VNode => {
  return slideTag('opponents')([
    hl('div.recap--massive', 'Your best chess foes'),
    hl(
      'table.recap__data',
      hl(
        'tbody',
        r.games.opponents.map(o =>
          hl('tr', [hl('td', opponentLink(o.value)), hl('td', [animateNumber(o.count), ' games'])]),
        ),
      ),
    ),
  ]);
};

const opponentLink = (o: LightUser): VNode =>
  hl('a', { attrs: { href: `/@/${o.name}` } }, [userFlair(o) || noFlair(o), userTitle(o), o.name]);

const userFallbackFlair = new Map<string, string>();
const noFlair = (o: LightUser): VNode => {
  const randomFlair =
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
  return hl('img.uflair.noflair', { attrs: { src: site.asset.flairSrc(randomFlair) } });
};

export const firstMoves = (r: Recap, firstMove: Counted<string>): VNode => {
  const percent = Math.round((firstMove.count * 100) / r.games.nbWhite);
  return slideTag('first')([
    hl('div.recap--massive', [hl('strong.animated-pulse', '1. ' + firstMove.value)]),
    hl('div', [
      hl('p', [
        'is how you started ',
        hl('strong', animateNumber(firstMove.count)),
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
    hl('div.lpv.lpv--todo.lpv--moves-bottom.is2d', {
      hook: onInsert(el => loadOpeningLpv(el, color, o.value)),
    }),
    hl(
      'div',
      hl(
        'a',
        {
          attrs: { href: `/opening/${o.value.key}`, target: '_blank' },
        },
        o.value.name,
      ),
    ),
    hl('div', [
      hl('p', [
        'Your most played opening as ',
        color,
        hl('br'),
        'with ',
        hl('strong', animateNumber(o.count)),
        ' games.',
      ]),
    ]),
  ]);
};

export const puzzles = (r: Recap): VNode => {
  return slideTag('puzzles')(
    r.puzzles.nbs.total
      ? [
          hl('div.recap--massive', [hl('strong', animateNumber(r.puzzles.nbs.total)), 'puzzles solved']),
          hl('div', [
            r.puzzles.nbs.win &&
              hl('p', [
                'You won ',
                hl('strong', animateNumber(r.puzzles.nbs.win)),
                ' of them on the first try!',
              ]),
            r.puzzles.votes.nb
              ? hl('p', [
                  'Thank you for voting on ',
                  hl('strong', animateNumber(r.puzzles.votes.nb)),
                  ' puzzles.',
                ])
              : null,
            r.puzzles.votes.themes
              ? hl('p', [
                  'You also helped tagging ',
                  hl('strong', animateNumber(r.puzzles.votes.themes)),
                  ' of them.',
                ])
              : null,
          ]),
        ]
      : [
          hl('div.recap--massive', 'You did not solve any puzzles this year.'),
          hl(
            'div',
            hl('p', hl('a', { attrs: { href: '/training', target: '_blank' } }, 'Wanna try some now?')),
          ),
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
      hl('div.recap--massive', 'Where did you find games?'),
      hl(
        'table.recap__data',
        hl(
          'tbody',
          best.map(
            ([n, c]) =>
              c > 0 && hl('tr', [hl('td', n), hl('td', [hl('strong', animateNumber(c)), ' games'])]),
          ),
        ),
      ),
    ])
  );
};

export const perfs = (r: Recap): VNode => {
  return slideTag('perfs')([
    hl('div.recap--massive', 'What time controls and variants did you play?'),
    hl(
      'table.recap__data',
      hl(
        'tbody',
        r.games.perfs.map(p =>
          hl('tr', [hl('td', renderPerf(p)), hl('td', [hl('strong', animateNumber(p.games)), ' games'])]),
        ),
      ),
    ),
  ]);
};

export const malware = (): VNode =>
  slideTag('malware')([
    hl('div.recap--massive', [hl('strong.animated-pulse', '0'), 'ads and trackers loaded']),
    hl('ul', [
      hl('li', "We didn't sell your personal data"),
      hl('li', "We didn't use your device against you"),
    ]),
    hl(
      'p',
      hl('small', [
        'But other websites do, so please ',
        hl('a', { attrs: { href: '/ads', target: '_blank' } }, 'be careful.'),
      ]),
    ),
  ]);

export const lichessGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = (r.games.nbs.total * 100) / totalGames;
  const showGamesPercentOfTotal = gamesPercentOfTotal.toFixed(6) + '%';
  return slideTag('lichess-games')([
    hl('div.recap--massive', [
      hl('strong', animateNumber(totalGames)),
      'games played on Lichess in ',
      r.year,
    ]),
    hl('div', [hl('p', [hl('strong', showGamesPercentOfTotal), ' of them are yours.'])]),
  ]);
};

export const thanks = (): VNode =>
  slideTag('thanks')([
    hl('div.recap--massive', 'Thank you for playing on Lichess!'),
    hl('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    hl('div', "We're glad you're here. Have a great 2025!"),
  ]);

const renderPerf = (perf: RecapPerf): VNode => {
  return hl('span', [
    hl('i.text', { attrs: dataIcon(perfIcons[perf.key]) }),
    perfNames[perf.key] || perf.key,
  ]);
};

const stat = (value: string | VNode, label: string): VNode =>
  hl('div.stat', [hl('div', hl('strong', value)), hl('div', hl('small', label))]);

export const shareable = (r: Recap): VNode =>
  slideTag('shareable')([
    hl('div.recap__shareable', [
      hl('img.logo', { attrs: { src: site.asset.url('logo/logo-with-name-dark.png') } }),
      hl('h2', 'My 2024 Recap'),
      hl('div.grid', [
        stat(formatNumber(r.games.nbs.total), 'games played'),
        stat(formatNumber(r.games.moves), 'moves played'),
        stat(formatDuration(r.games.timePlaying, ' and '), 'spent playing'),
        r.games.perfs[0]?.games && stat(renderPerf(r.games.perfs[0]), perfLabel(r.games.perfs[0])),
        r.games.opponents.length && stat(opponentLink(r.games.opponents[0].value), 'most played opponent'),
        stat(formatNumber(r.puzzles.nbs.total), 'puzzles solved'),
      ]),
      hl('div.openings', [
        r.games.openings.white.count && stat(r.games.openings.white.value.name, 'as white'),
        r.games.openings.black.count && stat(r.games.openings.black.value.name, 'as black'),
      ]),
    ]),
  ]);

const slideTag =
  (key: string, millis: number = 5000) =>
  (content: LooseVNodes) =>
    hl(
      `div.swiper-slide.recap__slide--${key}`,
      {
        attrs: {
          'data-swiper-autoplay': millis,
        },
      },
      content,
    );

const animateNumber = (n: number) => hl('span.animated-number', { attrs: { 'data-value': n } }, '0');
const animateTime = (n: number) => hl('span.animated-time', { attrs: { 'data-value': n } }, '');

const showGrams = (g: number) =>
  g > 20_000 ? hl('span', [animateNumber(g / 1000), ' Kilograms']) : hl('span', [animateNumber(g), ' grams']);
