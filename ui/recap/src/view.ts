import type { Opts, Recap } from './interfaces';
import { type VNode } from 'snabbdom';
import { looseH as h } from 'common/snabbdom';
import * as slides from './slides';

export function awaiter(user: LightUser): VNode {
  return h('div#recap-swiper.swiper.swiper-initialized', [h('div.swiper-wrapper', [slides.loading(user)])]);
}

export function view(r: Recap, opts: Opts): VNode {
  return h('div#recap-swiper.swiper', [
    h('div.swiper-wrapper', [
      slides.init(opts.user),
      ...(r.games.nbs.total
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
        : [slides.noGames()]),
      slides.puzzles(r),
      slides.malware(),
      slides.lichessGames(r),
      slides.thanks(),
      slides.shareable(r),
    ]),
    ...(opts.navigation ? [h('div.swiper-button-next'), h('div.swiper-button-prev')] : []),
    h('div.swiper-pagination'),
    h('div.autoplay-progress', [
      h('svg', { attrs: { viewBox: '0 0 48 48' } }, [h('circle', { attrs: { cx: 24, cy: 24, r: 20 } })]),
      h('span'),
    ]),
  ]);
}
