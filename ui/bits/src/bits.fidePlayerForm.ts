import { wireCropDialog } from './crop';

site.load.then(() => {
  $('.fide-player__photo-edit').each(function (this: HTMLElement) {
    const form = this;
    wireCropDialog({
      aspectRatio: 1,
      post: { url: $(form).data('post-url')!, field: 'photo' },
      selectClicks: $('.select-image'),
    });
  });
});
