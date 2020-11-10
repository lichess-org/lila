import sparkline from "@fnando/sparkline";
import { Controller, Puzzle, PuzzleGame, MaybeVNode } from '../interfaces';
import { dataIcon } from '../util';
import { h, thunk } from 'snabbdom';
import { numberFormat } from 'common/number';
import { VNode } from 'snabbdom/vnode';

export function puzzleBox(ctrl: Controller): VNode {
  var data = ctrl.getData();
  return h('div.puzzle__side__metas', [
    puzzleInfos(ctrl, data.puzzle),
    gameInfos(ctrl, data.game, data.puzzle)
  ]);
}

function puzzleInfos(ctrl: Controller, puzzle: Puzzle): VNode {
  return h('div.infos.puzzle', {
    attrs: dataIcon('-')
  }, [h('div', [
    h('a.title', {
      attrs: { href: '/training/' + puzzle.id }
    }, ctrl.trans('puzzleId', puzzle.id)),
    h('p', ctrl.trans.vdom('ratingX', ctrl.vm.mode === 'play' ? h('span.hidden', ctrl.trans.noarg('hidden')) : h('strong', puzzle.rating))),
    h('p', ctrl.trans.vdom('playedXTimes', h('strong', numberFormat(puzzle.attempts))))
  ])]);
}

function gameInfos(ctrl: Controller, game: PuzzleGame, puzzle: Puzzle): VNode {
  return h('div.infos', {
    attrs: dataIcon(game.perf.icon)
  }, [h('div', [
    h('p', ctrl.trans.vdom('fromGameLink', h('a', {
      attrs: { href: `/${game.id}/${puzzle.color}#${puzzle.initialPly}` }
    }, '#' + game.id))),
    h('p', [
      game.clock, ' • ',
      game.perf.name, ' • ',
      ctrl.trans.noarg(game.rated ? 'rated' : 'casual')
    ]),
    h('div.players', game.players.map(function(p) {
      return h('div.player.color-icon.is.text.' + p.color,
        p.userId ? h('a.user-link.ulpt', {
          attrs: { href: '/@/' + p.userId }
        }, p.name) : p.name
      );
    }))
  ])]);
}

export function userBox(ctrl: Controller): MaybeVNode {
  const data = ctrl.getData();
  if (!data.user) return;
  const diff = ctrl.vm.round && ctrl.vm.round.ratingDiff;
  const hash = ctrl.recentHash();
  return h('div.puzzle__side__user', [
    h('h2', ctrl.trans.vdom('yourPuzzleRatingX', h('strong', [
      data.user.rating,
      ...(diff >= 0 ? [' ', h('good.rp', '+' + diff)] : []),
      ...(diff < 0 ? [' ', h('bad.rp', '−' + (-diff))] : [])
    ]))),
    h('div', thunk('div.rating_chart.' + hash, ratingChart, [ctrl, hash]))
  ]);
}

function ratingChart(ctrl: Controller, hash: string): VNode {
  return h('div.rating_chart.' + hash, {
    hook: {
      insert(vnode) { drawRatingChart(ctrl, vnode) },
      postpatch(_, vnode) { drawRatingChart(ctrl, vnode) }
    }
  });
}

function drawRatingChart(ctrl: Controller, vnode: VNode): void {
  const $el = $(vnode.elm as HTMLElement);
  const points = ctrl.getData().user!.recent.map(r => r[2] + r[1]);
  const localPuzzleMin = Math.min(...points); 
  const redraw = () => {
    const $svg = $('<svg class="sparkline" height="80px" stroke-width="3">')
      .attr('width', Math.round($el.outerWidth()) + 'px')
      .prependTo($(vnode.elm).empty());
    sparkline($svg[0] as unknown as SVGSVGElement, points.map(r => r - localPuzzleMin), {
      interactive: true,
    })
  };
  requestAnimationFrame(redraw);
  window.addEventListener('resize', redraw);
}
