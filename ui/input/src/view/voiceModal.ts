import { MoveCtrl } from '../interfaces';
import { snabModal } from 'common/modal';
import { h } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import * as xhr from 'common/xhr';
import { voiceMoveCtrl } from '../voiceMoveCtrl';

export function voiceModal(ctrl: MoveCtrl) {
  return snabModal({
    class: `voice-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.modalOpen(false),
    onInsert: async el => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('inputMove.help'),
        xhr.text(xhr.url(`/help/voice-move`, {})),
      ]);
      el.find('.scrollable').html(html);
      el.find('#all-phrases-button').on('click', () => {
        let html = '<table id="big-table"><tbody>';
        const all = voiceMoveCtrl()
          .getAllAvailable()
          .sort((a, b) => a[0].localeCompare(b[0]));
        const cols = Math.min(3, Math.floor(window.innerWidth / 400 + 1));
        const rows = Math.ceil(all.length / cols);
        for (let i = 0; i < rows; i++) {
          html += '<tr>';

          for (let j = 0; j < cols && j * rows + i < all.length; j++) {
            const [a, b] = all[j * rows + i];
            html += `<td>${a}</td><td>${b}</td>`;
          }
          html += '</tr>';
        }
        html += '</tbody></table>';
        $('#modal-wrap').toggleClass('bigger');
        el.find('.scrollable').html(html);
      });
    },
  });
}
