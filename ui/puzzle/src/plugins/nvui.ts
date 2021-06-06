import { h, VNode } from 'snabbdom';
import { Controller, Redraw } from '../interfaces';
import { puzzleBox } from '../view/side';
import theme from '../view/theme';
import { renderPieces, styleSetting } from 'nvui/chess';
import { Chessground } from 'chessground';
import { makeConfig } from '../view/chessground';
import { renderSetting } from 'nvui/setting';

lichess.PuzzleNVUI = function (redraw: Redraw) {
  const moveStyle = styleSetting();

  return {
    render(ctrl: Controller): VNode {
      let ground = ctrl.ground();
      if (!ground) {
        ground = Chessground(document.createElement('div'), {
          ...makeConfig(ctrl),
          animation: { enabled: false },
          drawable: { enabled: false },
          coordinates: false,
        });
        ctrl.ground(ground);
      }
      const pieces = ground.state.pieces;

      return h(
        `main.puzzle.puzzle-${ctrl.getData().replay ? 'replay' : 'play'}${ctrl.streak ? '.puzzle--streak' : ''}`,
        [
          h('div.nvui', [
            h('h1', `You play the ${ctrl.vm.pov} pieces. ${ctrl.difficulty || ''} puzzle`),
            h('h2', 'Puzzle info'),
            puzzleBox(ctrl),
            theme(ctrl),
            h('h2', 'Pieces'),
            h('div.pieces', renderPieces(pieces, moveStyle.get())),
            h('h2', 'Settings'),
            h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          ]),
        ]
      );
    },
  };
};
