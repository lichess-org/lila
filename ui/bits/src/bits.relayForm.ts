import { wireCropDialog } from './crop';
import { createSelectSearch } from './selectSearch';

site.load.then(() => {
  if ($('#form3-markdown').length) {
    // tournament form

    $('#form3-info_timeZone').each(function (this: HTMLSelectElement) {
      const newForm = $('.form3[action="/broadcast/new"]');
      if (newForm.length && !newForm.find('.is-invalid').length)
        this.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
      createSelectSearch(this);
    });

    $('select[id^="form3-tiebreaks_"]').each(function (this: HTMLSelectElement) {
      createSelectSearch(this);
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

    const $label = $(`label[for="form3-delay"]`);
    const $delay = $('#form3-delay');
    const convertDelay = () => {
      const seconds = parseInt($delay.val() as string, 10);
      if (isNaN(seconds) || seconds <= 0) {
        $label.find('span').remove();
        return;
      }
      const minutes = Math.floor(seconds / 60);
      const remainingSeconds = seconds % 60;
      const delayText = ` (${minutes}m${remainingSeconds}s)`;
      const $span = $label.find('span');
      if ($span.length === 0) {
        $label.append($('<span>').text(delayText));
      } else {
        $span.text(delayText);
      }
    };

    $delay.on('input', convertDelay);
    convertDelay();
  }
});
