import { wireCropDialog } from './load/crop';

site.load.then(() => {
  wireCropDialog({
    post: { url: $('.title-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
});
