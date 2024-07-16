import { wireCropDialog } from './exports/crop';

site.load.then(() => {
  $('.title-image-edit').each(function (this: HTMLElement) {
    wireCropDialog({
      post: { url: $(this).attr('data-post-url')!, field: 'image' },
      selectClicks: $(this).find('.drop-target'),
      selectDrags: $(this).find('.drop-target'),
    });
  });
});
