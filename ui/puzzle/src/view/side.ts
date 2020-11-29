import { bind, dataIcon } from '../util';
import { Controller, Puzzle, PuzzleGame, MaybeVNode, OnComplete } from '../interfaces';
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
    h('p', ctrl.trans.vdom('puzzleId', h('a', {
      attrs: { href: `/training/${puzzle.id}` }
    }, '#' + puzzle.id))),
    h('p', ctrl.trans.vdom('ratingX', ctrl.vm.mode === 'play' ? h('span.hidden', ctrl.trans.noarg('hidden')) : h('strong', puzzle.rating))),
    h('p', ctrl.trans.vdom('playedXTimes', h('strong', numberFormat(puzzle.plays))))
  ])]);
}

function gameInfos(ctrl: Controller, game: PuzzleGame, puzzle: Puzzle): VNode {
  return h('div.infos', {
    attrs: dataIcon(game.perf.icon)
  }, [h('div', [
    h('p', ctrl.trans.vdom('fromGameLink', ctrl.vm.mode == 'play' ? h('span.hidden', ctrl.trans.noarg('hidden')) : h('a', {
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

const onCompleteOptions = [
  ['pause', 'Pause after each puzzle'],
  ['nextIfWin', 'Only pause when I get it wrong'],
  ['next', 'Always jump to next puzzle']
];

const completeSelector = (ctrl: Controller) => {
  const current = ctrl.onComplete();
  return h('select', {
    hook: bind('change', e => ctrl.onComplete((e.target as HTMLInputElement).value as OnComplete))
  }, onCompleteOptions.map(v =>
    h('option', {
      attrs: {
        value: v[0],
        selected: v[0] == current
      },
    }, v[1])
  )
  );
}

export function config(ctrl: Controller): MaybeVNode {
  return h('div.puzzle__side__config', [
    thunk('select.puzzle__side__config__selector', completeSelector, [ctrl])
  ]);
}
