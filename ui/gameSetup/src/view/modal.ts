import { MaybeVNode } from 'common/snabbdom';
import { snabDialog } from 'common/dialog';
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
  return snabDialog({
    class: ctrl.gameType === 'local' ? 'game-setup.local-setup' : 'game-setup',
    cssPath: 'game-setup',
    onClose: ctrl.closeModal,
    vnodes: renderContent(ctrl),
  });
}
