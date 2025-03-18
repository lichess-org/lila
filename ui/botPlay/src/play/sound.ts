import { SoundEvent } from 'local';
import PlayCtrl from './playCtrl';
import { opposite } from 'chessops';

export const playMoveSounds = async (ctrl: PlayCtrl, san: San) => {
  const justPlayed = opposite(ctrl.board.chess.turn);
  const sounds: SoundEvent[] = [];
  const prefix = ctrl.board.chess.turn === ctrl.game.pov ? 'bot' : 'player';
  if (san.includes('x')) sounds.push(`${prefix}Capture`);
  if (ctrl.board.chess.isCheck()) sounds.push(`${prefix}Check`);
  if (ctrl.game.end) sounds.push(`${prefix}Win`);
  sounds.push(`${prefix}Move`);
  const bridge = await ctrl.opts.bridge;
  const boardSoundVolume = sounds.length ? bridge.playSound(justPlayed, sounds) : 1;
  if (boardSoundVolume) site.sound.move({ san, volume: boardSoundVolume });
};
