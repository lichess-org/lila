import { wireCropDialog } from './crop';

site.load.then(() => {
  if ($('#form3-markdown').length) {
    // tournament form

    $('.form3[action="/broadcast/new"] #form3-info_timeZone').each(function (this: HTMLSelectElement) {
      if (!$('.is-invalid').length) this.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
    });

    wireCropDialog({
      aspectRatio: 2 / 1,
      post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
      selectClicks: $('.select-image, .drop-target'),
      selectDrags: $('.drop-target'),
    });
  } else {
    // round form

    const $source = $('#form3-syncSource');
    const showSource = () =>
      $('.relay-form__sync').each(function (this: HTMLElement) {
        this.classList.toggle('none', !this.classList.contains(`relay-form__sync-${$source.val()}`));
      });

    $source.on('change', showSource);
    showSource();
  }
});
