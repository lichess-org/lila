import { isSafari } from 'common/device';
import { wireCropDialog } from 'common/controls';

if (isSafari()) wireCropDialog(); // preload

lichess.load.then(() =>
  wireCropDialog({
    aspectRatio: 2 / 1,
    post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  }),
);
