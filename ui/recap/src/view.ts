import { type VNode } from 'snabbdom';

import { div, hl, span } from 'lib/view';

import type { Opts, Recap } from './interfaces';
import * as slides from './slides';

export function awaiter(user: LightUser): VNode {
  return div('#recap-swiper.swiper.swiper-initialized', [div('.swiper-wrapper', [slides.loading(user)])]);
}

export function view(r: Recap, opts: Opts): VNode {
  return div('#recap-swiper.swiper', [
    div('.swiper-wrapper', [
      slides.init(opts.user),
      r.games.nbs.total
        ? [
            slides.nbGames(r),
            slides.timeSpentPlaying(r),
            slides.nbMoves(r),
            slides.perfs(r),
            slides.sources(r),
            slides.opponents(r),
            r.games.firstMoves[0] && slides.firstMoves(r, r.games.firstMoves[0]),
            slides.openingColor(r.games.openings, 'white'),
            slides.openingColor(r.games.openings, 'black'),
          ]
        : slides.noGames(),
      slides.puzzles(r),
      slides.lichessGames(r),
      slides.malware(),
      slides.patron(opts),
      slides.thanks(r),
      slides.shareable(r),
    ]),
    opts.navigation ? [div('.swiper-button-next'), div('.swiper-button-prev')] : null,
    div('.swiper-pagination'),
    div('.autoplay-progress', [
      hl('svg', { attrs: { viewBox: '0 0 48 48' } }, hl('circle', { attrs: { cx: 24, cy: 24, r: 20 } })),
      span(),
    ]),
  ]);
}
