import { MaybeVNode } from 'common/snabbdom';
import { snabDialog } from 'common/dialog';
import LobbyController from '../../ctrl';
import hookContent from './hookContent';
import friendContent from './friendContent';
import aiContent from './aiContent';

const gameTypeToRenderer = {
  hook: hookContent,
  friend: friendContent,
  ai: aiContent,
};

export default function setupModal(ctrl: LobbyController): MaybeVNode {
  const { setupCtrl } = ctrl;
  if (!setupCtrl.gameType) return null;
  const renderContent = gameTypeToRenderer[setupCtrl.gameType];
  return snabDialog({
    class: 'game-setup',
    cssPath: 'lobby.setup',
    onClose: setupCtrl.closeModal,
    vnodes: renderContent(ctrl),
  });
}
