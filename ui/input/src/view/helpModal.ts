import { MoveCtrl } from '../interfaces';
import { snabModal } from 'common/modal';
import { h } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import * as xhr from 'common/xhr';

export function helpModal(ctrl: MoveCtrl) {
  const tpe = ctrl.root.keyboard && !ctrl.voice.isRecording ? 'keyboard' : 'voice';
  return snabModal({
    class: `${tpe}-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.helpModalOpen(false),
    onInsert: async $ => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('inputMove.help'),
        xhr.text(xhr.url(`/help/${tpe}-move`, {})),
      ]);
      $.find('.scrollable').html(html);
    },
  });
}
