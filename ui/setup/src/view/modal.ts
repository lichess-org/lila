import { VNode } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import hookContent from './hookContent';
import friendContent from './friendContent';
import aiContent from './aiContent';

const gameTypeToRenderer = {
  hook: hookContent,
  friend: friendContent,
  ai: aiContent,
};

export default function setupModal(ctrl: SetupCtrl): VNode {
  if (!ctrl.gameType || ctrl.gameType === 'local') return null;
  const renderContent = gameTypeToRenderer[ctrl.gameType];
  return snabDialog({
    class: 'game-setup',
    css: [{ url: 'game-setup' }],
    onClose: ctrl.closeModal,
    vnodes: renderContent(ctrl),
  });
}
