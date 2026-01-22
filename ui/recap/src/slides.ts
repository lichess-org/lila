import { pieceGrams, totalGames } from './constants';
import type { Counted, Opening, Recap, Sources, RecapPerf, Opts } from './interfaces';
import { onInsert, hl, type LooseVNodes, type VNode, dataIcon, spinnerVdom } from 'lib/view';
import { loadOpeningLpv } from './ui';
import { shuffle } from 'lib/algo';
import { fullName, userFlair, userTitle } from 'lib/view/userLink';
import { formatDuration, perfIsSpeed, perfLabel } from './util';
import perfIcons from 'lib/game/perfIcons';
import * as licon from 'lib/licon';
import { currencyFormat, numberFormat, percentFormat } from 'lib/i18n';
import { COLORS } from 'chessops';

const confettiCanvas = (): VNode =>
  hl('canvas#confetti', {
    hook: {
      insert: _ =>
        site.asset.loadEsm('bits.confetti', {
          init: {
            cannons: false,
            fireworks: true,
          },
        }),
    },
  });

const hi = (user: LightUser): VNode => hl('h2', i18n.recap.hiUser.asArray(fullName(user)));

export const loading = (user: LightUser): VNode =>
  slideTag('await')([hi(user), hl('p', i18n.recap.awaitQuestion), spinnerVdom()]);

export const init = (user: LightUser): VNode =>
  slideTag('init')([
    confettiCanvas(),
    hi(user),
    hl('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    hl('h2', i18n.recap.initTitle),
  ]);

export const noGames = (): VNode =>
  slideTag('no-games')([
    hl('div.recap--massive', i18n.recap.noGamesText),
    hl('div', hl('p', hl('a', { attrs: { href: '/', target: '_blank' } }, i18n.recap.noGamesCta))),
  ]);

export const nbGames = (r: Recap): VNode => {
  return slideTag('games')([
    hl(
      'div.recap--massive',
      i18n.site.nbGames.asArray(r.games.nbs.total, hl('strong', animateNumber(r.games.nbs.total))),
    ),
    hl('div', [
      r.games.nbs.win &&
        hl('p', i18n.recap.gamesYouWon.asArray(hl('strong', animateNumber(r.games.nbs.win)))),
      hl('p', i18n.recap.gamesNextQuestion),
    ]),
  ]);
};

export const timeSpentPlaying = (r: Recap): VNode => {
  const s = r.games.timePlaying;
  const days = s / 60 / 60 / 24;
  return slideTag('time')([
    hl('div.recap--massive', i18n.recap.timeSpentPlayingExclam.asArray(hl('strong', animateTime(s)))),
    hl('div', [
      hl(
        'p',
        days > 10 ? i18n.recap.timeTooMuch : days > 5 ? i18n.recap.timeALot : i18n.recap.timeReasonable,
      ),
      hl('p', i18n.recap.timeHowManyMoves),
    ]),
  ]);
};

export const nbMoves = (r: Recap): VNode => {
  return slideTag(
    'moves',
    6000,
  )([
    hl(
      'div.recap--massive',
      i18n.recap.nbMoves.asArray(r.games.moves, hl('strong', animateNumber(r.games.moves))),
    ),
    hl('div', [
      hl('p', i18n.recap.movesOfWoodPushed.asArray(showGrams(r.games.moves * pieceGrams))),
      hl('p', [hl('small', i18n.recap.movesStandardPiecesWeight)]),
    ]),
  ]);
};

export const opponents = (r: Recap): VNode => {
  return slideTag('opponents')([
    hl('div.recap--massive', i18n.recap.chessFoes),
    hl(
      'table.recap__data',
      hl(
        'tbody',
        r.games.opponents.map(o =>
          hl('tr', [
            hl('td', opponentLink(o.value)),
            hl('td', i18n.site.nbGames.asArray(o.count, animateNumber(o.count))),
          ]),
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
  const ofTotal = firstMove.count / r.games.nbWhite;
  return slideTag('first')([
    hl('div.recap--massive', [hl('strong.animated-pulse', '1. ' + firstMove.value)]),
    hl('div', [
      hl(
        'p',
        i18n.recap.firstMoveStats.asArray(
          hl('div', [hl('strong', animateNumber(firstMove.count)), ` (${percentFormat(ofTotal, 2)})`]),
        ),
      ),
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
      hl(
        'p',
        i18n.recap[color === 'white' ? 'openingsMostPlayedAsWhite' : 'openingsMostPlayedAsBlack'].asArray(
          o.count,
          hl('strong', animateNumber(o.count)),
        ),
      ),
    ]),
  ]);
};

export const puzzles = (r: Recap): VNode => {
  return slideTag('puzzles')(
    r.puzzles.nbs.total
      ? [
          hl(
            'div.recap--massive',
            i18n.site.nbPuzzles.asArray(
              r.puzzles.nbs.total,
              hl('strong', animateNumber(r.puzzles.nbs.total)),
            ),
          ),
          hl('div', [
            !!r.puzzles.nbs.win &&
              hl(
                'p',
                i18n.recap.puzzlesYouWonOnFirstTry.asArray(hl('strong', animateNumber(r.puzzles.nbs.win))),
              ),
            !!r.puzzles.votes.nb &&
              hl(
                'p',
                i18n.recap.puzzlesThanksVoting.asArray(
                  r.puzzles.votes.nb,
                  hl('strong', animateNumber(r.puzzles.votes.nb)),
                ),
              ),
            !!r.puzzles.votes.themes &&
              hl(
                'p',
                i18n.recap.puzzlesHelpedTagging.asArray(hl('strong', animateNumber(r.puzzles.votes.themes))),
              ),
          ]),
        ]
      : [
          hl('div.recap--massive', i18n.recap.puzzlesNone),
          hl(
            'div',
            hl('p', hl('a', { attrs: { href: '/training', target: '_blank' } }, i18n.recap.puzzlesTryNow)),
          ),
        ],
  );
};

export const sources = (r: Recap): VNode => {
  const all: [keyof Sources, string][] = [
    ['friend', i18n.preferences.notifyChallenge],
    ['ai', i18n.site.computer],
    ['arena', i18n.arena.arenaTournaments],
    ['swiss', i18n.swiss.swissTournaments],
    ['simul', i18n.site.simultaneousExhibitions],
    ['pool', i18n.site.quickPairing],
    ['lobby', i18n.site.lobby],
  ];
  const best: [string, number][] = all.map(([k, n]) => [n, r.games.sources[k] || 0]);
  best.sort((a, b) => b[1] - a[1]);
  return (
    best[0] &&
    slideTag('sources')([
      hl('div.recap--massive', i18n.recap.sourcesTitle),
      hl(
        'table.recap__data',
        hl(
          'tbody',
          best.map(
            ([n, c]) =>
              c > 0 &&
              hl('tr', [hl('td', n), hl('td', i18n.site.nbGames.asArray(c, hl('strong', animateNumber(c))))]),
          ),
        ),
      ),
    ])
  );
};

export const perfs = (r: Recap): VNode => {
  return slideTag('perfs')([
    hl('div.recap--massive', i18n.recap.perfsTitle),
    hl(
      'table.recap__data',
      hl(
        'tbody',
        r.games.perfs.map(p =>
          hl('tr', [
            hl('td', renderPerf(p)),
            hl('td', i18n.site.nbGames.asArray(p.games, hl('strong', animateNumber(p.games)))),
          ]),
        ),
      ),
    ),
  ]);
};

export const malware = (): VNode =>
  slideTag('malware')([
    hl('div.recap--massive', i18n.recap.malwareNoneLoaded.asArray(hl('strong.animated-pulse', '0'))),
    hl('ul', [hl('li', i18n.recap.malwareNoSell), hl('li', i18n.recap.malwareNoAbuse)]),
    hl(
      'p',
      hl('small', [
        i18n.recap.malwareWarningPrefix.asArray(
          hl('a', { attrs: { href: '/ads', target: '_blank' } }, i18n.recap.malwareWarningCta),
        ),
      ]),
    ),
  ]);

export const lichessGames = (r: Recap): VNode => {
  const gamesPercentOfTotal = r.games.nbs.total / totalGames;
  return slideTag('lichess-games')([
    hl(
      'div.recap--massive',
      i18n.recap.lichessGamesPlayedIn.asArray<LooseVNodes>(hl('strong', animateNumber(totalGames)), r.year),
    ),
    hl(
      'div',
      hl(
        'p',
        i18n.recap.lichessGamesOfThemYours.asArray(hl('strong', percentFormat(gamesPercentOfTotal, 6))),
      ),
    ),
  ]);
};

export const thanks = (r: Recap): VNode =>
  slideTag('thanks')([
    hl('div.recap--massive', i18n.recap.thanksTitle),
    hl('img.recap__logo', { attrs: { src: site.asset.url('logo/lichess-white.svg') } }),
    hl('div', i18n.recap.thanksHaveAGreat.asArray(r.year + 1)),
  ]);

export const patron = (opts: Opts): VNode =>
  slideTag('patron')([
    hl(
      'div.recap--big',
      i18n.recap.patronCostsThisYear.asArray(
        hl('a', { attrs: { href: '/costs', target: '_blank' } }, i18n.recap.patronCosts),
        opts.costs &&
          hl(
            'strong',
            currencyFormat(opts.costs.amount, opts.costs.currency, {
              maximumFractionDigits: 0,
            }),
          ),
      ),
    ),
    hl('p', i18n.recap.patronCharity),
    hl('i.text', { attrs: dataIcon(licon.Wings) }),

    opts.user.patron
      ? hl('p', i18n.patron.thankYou)
      : hl(
          'p.cta',
          i18n.recap.patronConsiderDonating.asArray(
            hl('a', { attrs: { href: '/patron', target: '_blank' } }, i18n.recap.patronMakeDonation),
          ),
        ),
  ]);

const renderPerf = (perf: RecapPerf): VNode => {
  return hl('span', [
    hl('i.text', { attrs: dataIcon(perfIcons[perf.key]) }),
    !perfIsSpeed(perf.key)
      ? i18n.variant[perf.key]
      : perf.key !== 'ultraBullet'
        ? i18n.site[perf.key]
        : perf.key,
  ]);
};

const stat = (value: string | VNode, label: string): VNode =>
  hl('div.stat', [hl('div', hl('strong', value)), hl('div', hl('small', label))]);

const stati18n = (value: number, plural: I18nPlural): VNode => {
  const [_, num, label] = plural.asArray(value, hl('strong', numberFormat(value)));
  return hl('div.stat', [hl('div', num), hl('div', hl('small', label))]);
};

export const shareable = (r: Recap): VNode =>
  slideTag('shareable')([
    hl('div.recap__shareable', [
      hl('img.logo', { attrs: { src: site.asset.url('logo/logo-with-name-dark.png') } }),
      hl('h2', i18n.recap.shareableTitle.asArray(r.year)),
      hl('div.grid', [
        stati18n(r.games.nbs.total, i18n.site.nbGames),
        stati18n(r.games.moves, i18n.recap.nbMovesPlayed),
        stat(formatDuration(r.games.timePlaying, ', '), i18n.recap.shareableSpentPlaying),
        r.games.perfs[0]?.games && stat(renderPerf(r.games.perfs[0]), perfLabel(r.games.perfs[0])),
        r.games.opponents.length &&
          stat(opponentLink(r.games.opponents[0].value), i18n.recap.shareableMostPlayedOpponent),
        stati18n(r.puzzles.nbs.total, i18n.recap.shareableNbPuzzlesSolved),
      ]),
      hl(
        'div.openings',
        COLORS.map(
          c =>
            r.games.openings[c].count &&
            stat(r.games.openings[c].value.name, i18n.site[c === 'white' ? 'asWhite' : 'asBlack']),
        ),
      ),
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
  g > 20_000
    ? hl('span', i18n.recap.nbKilograms.asArray(Math.round(g / 1000), animateNumber(g / 1000)))
    : hl('span', i18n.recap.nbGrams.asArray(g, animateNumber(g)));
