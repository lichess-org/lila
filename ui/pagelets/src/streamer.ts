import * as xhr from 'common/xhr';
import { wireCropDialog } from './load/crop';

site.load.then(() => {
  $('.streamer-show, .streamer-list').on('change', '.streamer-subscribe input', (e: Event) => {
    const target = e.target as HTMLInputElement;
    $(target)
      .parents('.streamer-subscribe')
      .each(function (this: HTMLElement) {
        xhr.text(
          $(this)
            .data('action')
            .replace(/set=[^&]+/, `set=${target.checked}`),
          { method: 'post' },
        );
      });
  });
  $('.online-check').on('click', e => {
    const uid = (e.target as HTMLElement).dataset.streamerId;
    xhr.text(`/streamer-check${uid ? `?streamer=${uid}` : ''}`, { method: 'post' });
    site.dialog.dom({
      htmlText: `<p><strong>Checking ${uid ? uid + "'s" : 'your'} channel for a live stream.</strong></p>
      <p>You cannot check again until one minute has passed. This page will not refresh.</p>
      <p>Please reload in one minute and verify approval and lichess.org in your stream title.</p>`,
      show: 'modal',
    });
  });
  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/streamer', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
});
