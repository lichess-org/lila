import { renderKeyboardView } from './view/keyboardView';
import { renderVoiceView } from './view/voiceView';
import { MoveCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';
export { makeMoveCtrl } from './moveCtrl';
export { voiceCtrl } from 'common/src/voiceCtrl';

export function renderMoveCtrl(ctrl: MoveCtrl, withBg = false) {
  return ctrl.root.keyboard ? renderKeyboardView(ctrl) : renderVoiceView(ctrl, withBg);
}
