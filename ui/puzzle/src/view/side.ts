import { Controller, Puzzle, PuzzleGame, MaybeVNode } from '../interfaces';
import { dataIcon } from '../util';
import { h } from 'snabbdom';
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
    h('p', ctrl.trans.vdom('ratingX', ctrl.vm.mode === 'play' ? h('span.hidden', ctrl.trans.noarg('hidden')) : h('strong', puzzle.rating))),
    h('p', ctrl.trans.vdom('playedXTimes', h('strong', numberFormat(puzzle.plays))))
  ])]);
}

function gameInfos(ctrl: Controller, game: PuzzleGame, puzzle: Puzzle): VNode {
  return h('div.infos', {
    attrs: dataIcon(game.perf.icon)
  }, [h('div', [
    h('p', ctrl.trans.vdom('fromGameLink', h('a', {
      attrs: { href: `/${game.id}/${ctrl.vm.pov}#${puzzle.initialPly}` }
    }, '#' + game.id))),
    h('p', [
      game.clock, ' • ',
      game.perf.name, ' • ',
      ctrl.trans.noarg(game.rated ? 'rated' : 'casual')
    ]),
    h('div.players', game.players.map(p =>
      h('div.player.color-icon.is.text.' + p.color,
        p.userId ? h('a.user-link.ulpt', {
          attrs: { href: '/@/' + p.userId }
        }, p.name) : p.name
      )
    ))
  ])]);
}

export function userBox(ctrl: Controller): MaybeVNode {
  const data = ctrl.getData();
  if (!data.user) return;
  const diff = ctrl.vm.round?.ratingDiff;
  return h('div.puzzle__side__user', [
    h('p.puzzle__side__user__rating', ctrl.trans.vdom('yourPuzzleRatingX', h('strong', [
      data.user.rating,
      ...(diff && diff > 0 ? [' ', h('good.rp', '+' + diff)] : []),
      ...(diff && diff < 0 ? [' ', h('bad.rp', '−' + (-diff))] : [])
    ])))
  ]);
}
