import { wireCropDialog } from './exports/crop';

site.load.then(() => {
  wireCropDialog({
    aspectRatio: 2 / 1,
    post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });

  const $source = $('#form3-syncSource'),
    showSource = () =>
      $('.relay-form__sync').each(function (this: HTMLElement) {
        this.classList.toggle('none', !this.classList.contains(`relay-form__sync-${$source.val()}`));
      });

  $source.on('change', showSource);
  showSource();
});
