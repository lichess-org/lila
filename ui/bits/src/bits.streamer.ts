import * as xhr from 'common/xhr';
import { wireCropDialog } from './exports/crop';

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
  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/streamer', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
});
