import { makeCtrl, RootCtrl, VoiceMove } from '../main';

export function makeVoiceMove(ctrl: RootCtrl, fen: string): VoiceMove {
  let move: VoiceMove;
  const ui = makeCtrl({ redraw: ctrl.redraw, module: () => move, tpe: 'move' });

  lichess.loadModule('voice.move').then(() => (move = window.LichessVoiceMove(ctrl, ui, fen)));

  // shimmed so we can show the UI while fetching the module
  return {
    ui,
    initGrammar: () => move.initGrammar(),
    update: fen => move.update(fen),
    opponentRequest: (request, callback) => move.opponentRequest(request, callback),
    get promotionHook() {
      return move.promotionHook;
    },
    get allPhrases() {
      return move.allPhrases;
    },
    get prefNodes() {
      return move.prefNodes;
    },
  };
}
