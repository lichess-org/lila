import { h } from 'snabbdom';
import { VNode } from 'snabbdom';

export default function (ctrl: any): VNode {
  return h('div.puzzle-woodpecker', [
    h('div.puzzle__board.main-board', [
      // The chessboard will be rendered here by the puzzle module
    ]),
    h('div.puzzle__tools', [
      h('div.woodpecker-info', [
        h('h2', 'Woodpecker Training'),
        h('div.cycle-info', [
          'Cycle: ',
          h('strong', ctrl.cycle.toString())
        ]),
        h('div.progress', [
          `Puzzle ${ctrl.puzzleIndex + 1} of ${ctrl.puzzleCount}`
        ])
      ])
    ])
  ]);
}