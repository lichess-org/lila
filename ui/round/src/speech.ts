import RoundController from './ctrl';
import { Step } from './interfaces';

export const setup = (ctrl: RoundController) => {
  lichess.pubsub.on('speech.enabled', onSpeechChange(ctrl));
  onSpeechChange(ctrl)(lichess.sound.speech());
};

const onSpeechChange = (ctrl: RoundController) => (enabled: boolean) => {
  if (!window.LichessSpeech && enabled) lichess.loadModule('speech').then(() => status(ctrl));
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
};

const statusText = (ctrl: RoundController) => {
  const g = ctrl.data.game;
  switch (g.status.name) {
    case 'started':
      return 'Playing';
    case 'aborted':
      return 'Game aborted';
    case 'mate':
      return 'Checkmate';
    case 'resign':
      return `${g.winner} resigned`;
    case 'stalemate':
      return 'Stalemate';
    case 'timeout':
      switch (g.winner) {
        case 'white':
          return 'Black left the game';
        case 'black':
          return 'White left the game';
      }
      return 'Draw';
    case 'draw':
      return 'Draw';
    case 'outoftime':
      return `${g.turns % 2 === 0 ? 'White' : 'Black'} time out${g.winner ? '' : ' - Draw'}`;
    case 'noStart':
      return (g.winner == 'white' ? 'Black' : 'White') + " didn't move";
    case 'cheat':
      return 'Cheat detected';
    case 'variantEnd':
      switch (g.variant.key) {
        case 'kingOfTheHill':
          return 'King in the centre';
        case 'threeCheck':
          return 'Three checks';
      }
      return 'Variant ending';
    case 'unknownFinish':
      return 'Finished';
  }
};

export const status = (ctrl: RoundController) => {
  const text = statusText(ctrl);
  if (text === 'Playing') window.LichessSpeech!.step(ctrl.stepAt(ctrl.ply), false);
  else if (text) {
    withSpeech(speech => speech.say(text, false));
    const w = ctrl.data.game.winner;
    if (w) withSpeech(speech => speech.say(`${w} is victorious`, false));
  }
};

export const userJump = (ctrl: RoundController, ply: Ply) => withSpeech(s => s.step(ctrl.stepAt(ply), true));

export const step = (step: Step) => withSpeech(s => s.step(step, false));

const withSpeech = (f: (speech: LichessSpeech) => void) => window.LichessSpeech && f(window.LichessSpeech);
