import type { MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import type LobbyController from '../../../ctrl';

export const isEditingMessage = (ctrl: LobbyController): MaybeVNode => {

  if (site.blindMode || !ctrl.setupCtrl.isEditingPreference) return null;

  return h(
      'div.is-editing',
      "You are editing a challenge preference. Click on a king to confirm. Close the dialog to cancel"
    );
  }
