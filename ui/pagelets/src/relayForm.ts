import { isSafari } from 'common/device';
import { wire as wireCropDialog } from './crop';

if (isSafari()) wireCropDialog(); // preload

site.load.then(() =>
  wireCropDialog({
    aspectRatio: 2 / 1,
    post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  }),
);
