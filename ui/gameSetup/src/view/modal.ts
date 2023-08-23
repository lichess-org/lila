import { MaybeVNode } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { SetupCtrl } from '../ctrl';
import hookContent from './hookContent';
import friendContent from './friendContent';
import aiContent from './aiContent';
import localContent from './localContent';

const gameTypeToRenderer = {
  hook: hookContent,
  friend: friendContent,
  ai: aiContent,
  local: localContent,
};

export default function setupModal(ctrl: SetupCtrl): MaybeVNode {
  if (!ctrl.gameType) return null;
  const renderContent = gameTypeToRenderer[ctrl.gameType];
  return snabModal({
    class: 'game-setup',
    onInsert: () => lichess.loadCssPath('lobby.setup'),
    onClose: ctrl.closeModal,
    content: renderContent(ctrl),
  });
}
