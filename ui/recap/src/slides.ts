import { COLORS } from 'chessops';

import { shuffle } from 'lib/algo';
import perfIcons from 'lib/game/perfIcons';
import { currencyFormat, numberFormat, percentFormat } from 'lib/i18n';
import { licon } from 'lib/licon';
import {
  onInsert,
  hl,
  type VNode,
  spinnerVdom,
  icon,
  h2,
  p,
  img,
  div,
  a,
  strong,
  small,
  table,
  tbody,
  tr,
  td,
  ul,
  li,
  canvas,
  span,
} from 'lib/view';
import { fullName, profileUrl, userFlair, userTitle } from 'lib/view/userLink';

import { pieceGrams, totalGames } from './constants';
import type { Counted, Opening, Recap, Sources, RecapPerf, Opts } from './interfaces';
import { loadOpeningLpv } from './ui';
import { formatDuration, perfIsSpeed, perfLabel } from './util';

const confettiCanvas = (): VNode =>
  canvas('#confetti', {
    hook: onInsert(() => {
      site.asset.loadEsm('bits.confetti', {
        init: {
          cannons: false,
          fireworks: true,
        },
      });
    }),
  });

const hi = (user: LightUser): VNode => h2(i18n.recap.hiUser.asArray(fullName(user)));

export const loading = (user: LightUser): VNode =>
  slideTag('await')([hi(user), p(i18n.recap.awaitQuestion), spinnerVdom()]);

export const init = (user: LightUser): VNode =>
  slideTag('init')([
    confettiCanvas(),
    hi(user),
    img(site.asset.url('logo/lichess-white.svg'), 'Lichess logo')('.recap__logo'),
    h2(i18n.recap.initTitle),
  ]);

export const noGames = (): VNode =>
  slideTag('no-games')([
    div('.recap--massive', i18n.recap.noGamesText),
    div(p(a('/')({ target: '_blank' }, i18n.recap.noGamesCta))),
  ]);

export const nbGames = ({ games }: Recap): VNode => {
  return slideTag('games')([
    div(
      '.recap--massive',
      i18n.site.nbGames.asArray(games.nbs.total, strong(animateNumber(games.nbs.total))),
    ),
    div([
      games.nbs.win && p(i18n.recap.gamesYouWon.asArray(strong(animateNumber(games.nbs.win)))),
      p(i18n.recap.gamesNextQuestion),
    ]),
  ]);
};

export const timeSpentPlaying = ({ games }: Recap): VNode => {
  const s = games.timePlaying;
  const days = s / 60 / 60 / 24;
  return slideTag('time')([
    div('.recap--massive', i18n.recap.timeSpentPlayingExclam.asArray(strong(animateTime(s)))),
    div([
      p(days > 10 ? i18n.recap.timeTooMuch : days > 5 ? i18n.recap.timeALot : i18n.recap.timeReasonable),
      p(i18n.recap.timeHowManyMoves),
    ]),
  ]);
};

export const nbMoves = ({ games }: Recap): VNode => {
  return slideTag(
    'moves',
    6000,
  )([
    hl('div.recap--massive', i18n.recap.nbMoves.asArray(games.moves, strong(animateNumber(games.moves)))),
    div([
      p(i18n.recap.movesOfWoodPushed.asArray(showGrams(games.moves * pieceGrams))),
      p(small(i18n.recap.movesStandardPiecesWeight)),
    ]),
  ]);
};

export const opponents = ({ games }: Recap): VNode => {
  return slideTag('opponents')([
    div('.recap--massive', i18n.recap.chessFoes),
    table(
      '.recap__data',
      tbody(
        games.opponents.map(o =>
          tr([td(opponentLink(o.value)), td(i18n.site.nbGames.asArray(o.count, animateNumber(o.count)))]),
        ),
      ),
    ),
  ]);
};

const opponentLink = (o: LightUser): VNode =>
  a(profileUrl(o.name))([userFlair(o) ?? noFlair(o), userTitle(o), o.name]);

const userFallbackFlair = new Map<string, string>();
const noFlair = ({ id }: LightUser): VNode => {
  const randomFlair =
    userFallbackFlair.get(id) ||
    userFallbackFlair
      .set(
        id,
        (() =>
          shuffle([
            'activity.lichess-horsey',
            'activity.lichess-hogger',
            'activity.lichess-horsey-yin-yang',
          ])[0])(),
      )
      .get(id)!;
  return img(site.asset.flairSrc(randomFlair), 'Flair')('.uflair.noflair');
};

export const firstMoves = ({ games }: Recap, firstMove: Counted<string>): VNode => {
  const ofTotal = firstMove.count / games.nbWhite;
  return slideTag('first')([
    div('.recap--massive', [strong('.animated-pulse', '1. ' + firstMove.value)]),
    div(
      p(
        i18n.recap.firstMoveStats.asArray(
          div([strong(animateNumber(firstMove.count)), ` (${percentFormat(ofTotal, 2)})`]),
        ),
      ),
    ),
  ]);
};

export const openingColor = (os: ByColor<Counted<Opening>>, color: Color): VNode | undefined => {
  const o = os[color];
  if (!o.count) return undefined;
  return slideTag('openings')([
    div('.lpv.lpv--todo.lpv--moves-bottom.is2d', {
      hook: onInsert(el => loadOpeningLpv(el, color, o.value)),
    }),
    div(a(`/opening/${o.value.key}`)({ target: '_blank' }, o.value.name)),
    div(
      p(
        i18n.recap[color === 'white' ? 'openingsMostPlayedAsWhite' : 'openingsMostPlayedAsBlack'].asArray(
          o.count,
          strong(animateNumber(o.count)),
        ),
      ),
    ),
  ]);
};

export const puzzles = ({ puzzles }: Recap): VNode => {
  return slideTag('puzzles')(
    puzzles.nbs.total
      ? [
          div(
            '.recap--massive',
            i18n.site.nbPuzzles.asArray(puzzles.nbs.total, strong(animateNumber(puzzles.nbs.total))),
          ),
          div([
            puzzles.nbs.win
              ? p(i18n.recap.puzzlesYouWonOnFirstTry.asArray(strong(animateNumber(puzzles.nbs.win))))
              : null,
            puzzles.votes.nb
              ? p(
                  i18n.recap.puzzlesThanksVoting.asArray(
                    puzzles.votes.nb,
                    strong(animateNumber(puzzles.votes.nb)),
                  ),
                )
              : null,
            puzzles.votes.themes
              ? p(i18n.recap.puzzlesHelpedTagging.asArray(strong(animateNumber(puzzles.votes.themes))))
              : null,
          ]),
        ]
      : [
          div('.recap--massive', i18n.recap.puzzlesNone),
          div(p(a('/training')({ target: '_blank' }, i18n.recap.puzzlesTryNow))),
        ],
  );
};

export const sources = ({ games }: Recap): VNode => {
  const all: [keyof Sources, string][] = [
    ['friend', i18n.preferences.notifyChallenge],
    ['ai', i18n.site.computer],
    ['arena', i18n.arena.arenaTournaments],
    ['swiss', i18n.swiss.swissTournaments],
    ['simul', i18n.site.simultaneousExhibitions],
    ['pool', i18n.site.quickPairing],
    ['lobby', i18n.site.lobby],
  ];
  const best: [string, number][] = all.map(([k, n]) => [n, games.sources[k] ?? 0]);
  best.sort((a, b) => b[1] - a[1]);
  return (
    best[0] &&
    slideTag('sources')([
      div('.recap--massive', i18n.recap.sourcesTitle),
      table(
        '.recap__data',
        tbody(
          best.map(([n, c]) =>
            c > 0 ? tr([td(n), td(i18n.site.nbGames.asArray(c, strong(animateNumber(c))))]) : null,
          ),
        ),
      ),
    ])
  );
};

export const perfs = ({ games }: Recap): VNode => {
  return slideTag('perfs')([
    div('.recap--massive', i18n.recap.perfsTitle),
    table(
      '.recap__data',
      tbody(
        games.perfs.map(p =>
          tr([td(renderPerf(p)), td(i18n.site.nbGames.asArray(p.games, strong(animateNumber(p.games))))]),
        ),
      ),
    ),
  ]);
};

export const malware = (): VNode =>
  slideTag('malware')([
    div('.recap--massive', i18n.recap.malwareNoneLoaded.asArray(strong('0'))),
    ul([li(i18n.recap.malwareNoSell), li(i18n.recap.malwareNoAbuse)]),
    p(
      small(
        i18n.recap.malwareWarningPrefix.asArray(
          a('/ads')({ target: '_blank' }, i18n.recap.malwareWarningCta),
        ),
      ),
    ),
  ]);

export const lichessGames = ({ games, year }: Recap): VNode => {
  const gamesPercentOfTotal = games.nbs.total / totalGames;
  return slideTag('lichess-games')([
    div(
      '.recap--massive',
      i18n.recap.lichessGamesPlayedIn.asArray<VNode | number>(strong(animateNumber(totalGames)), year),
    ),
    div(p(i18n.recap.lichessGamesOfThemYours.asArray(strong(percentFormat(gamesPercentOfTotal, 6))))),
  ]);
};

export const thanks = ({ year }: Recap): VNode =>
  slideTag('thanks')([
    div('.recap--massive', i18n.recap.thanksTitle),
    img(site.asset.url('logo/lichess-white.svg'), 'Lichess logo')('.recap__logo'),
    div(i18n.recap.thanksHaveAGreat.asArray(year + 1)),
  ]);

export const patron = ({ costs, user }: Opts): VNode =>
  slideTag('patron')([
    div(
      '.recap--big',
      i18n.recap.patronCostsThisYear.asArray(
        a('/costs')({ target: '_blank' }, i18n.recap.patronCosts),
        costs &&
          strong(
            currencyFormat(costs.amount, costs.currency, {
              maximumFractionDigits: 0,
            }),
          ),
      ),
    ),
    p(i18n.recap.patronCharity),
    icon(licon.Wings)('.text'),
    user.patron
      ? p(i18n.patron.thankYou)
      : p(
          '.cta',
          i18n.recap.patronConsiderDonating.asArray(
            a('/patron')({ target: '_blank' }, i18n.recap.patronMakeDonation),
          ),
        ),
  ]);

const renderPerf = ({ key }: RecapPerf): VNode => {
  return span([
    icon(perfIcons[key])('.text'),
    !perfIsSpeed(key) ? i18n.variant[key] : key !== 'ultraBullet' ? i18n.site[key] : key,
  ]);
};

const stat = (value: string | VNode, label: string): VNode =>
  div('.stat', [div(strong(value)), div(small(label))]);

const stati18n = (value: number, plural: I18nPlural): VNode => {
  const [, num, label] = plural.asArray(value, strong(numberFormat(value)));
  return div('.stat', [div(num), div(small(label))]);
};

export const shareable = ({ games, year, puzzles }: Recap): VNode =>
  slideTag('shareable')([
    div('.recap__shareable', [
      img(site.asset.url('logo/logo-with-name-dark.png'), 'Lichess logo')('.logo'),
      h2(i18n.recap.shareableTitle.asArray(year)),
      div('.grid', [
        stati18n(games.nbs.total, i18n.site.nbGames),
        stati18n(games.moves, i18n.recap.nbMovesPlayed),
        stat(formatDuration(games.timePlaying, ', '), i18n.recap.shareableSpentPlaying),
        games.perfs[0]?.games && stat(renderPerf(games.perfs[0]), perfLabel(games.perfs[0])),
        games.opponents.length > 0
          ? stat(opponentLink(games.opponents[0].value), i18n.recap.shareableMostPlayedOpponent)
          : null,
        stati18n(puzzles.nbs.total, i18n.recap.shareableNbPuzzlesSolved),
      ]),
      div(
        '.openings',
        COLORS.map(c =>
          games.openings[c].count > 0
            ? stat(games.openings[c].value.name, i18n.site[c === 'white' ? 'asWhite' : 'asBlack'])
            : null,
        ),
      ),
    ]),
  ]);

const slideTag =
  (key: string, millis = 5000) =>
  (content: VNode[]) =>
    div(
      `.swiper-slide.recap__slide--${key}`,
      {
        'data-swiper-autoplay': millis,
      },
      content,
    );

const animateNumber = (n: number) => span('.animated-number', { attrs: { 'data-value': n } }, '0');
const animateTime = (n: number) => span('.animated-time', { attrs: { 'data-value': n } }, '');

const showGrams = (g: number) =>
  g > 20_000
    ? span(i18n.recap.nbKilograms.asArray(Math.round(g / 1000), animateNumber(g / 1000)))
    : span(i18n.recap.nbGrams.asArray(g, animateNumber(g)));
