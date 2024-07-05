import flatpickr from 'flatpickr';

site.load.then(() => {
  const $variant = $('#form3-variant'),
    showPosition = () =>
      $('.form3 .position').toggleClass('none', !['1', 'standard'].includes($variant.val() as string));

  $variant.on('change', showPosition);
  showPosition();

  $('.flatpickr').each(function (this: HTMLInputElement) {
    flatpickr(this, {
      minDate: 'today',
      maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 6),
      dateFormat: 'Z',
      altInput: true,
      altFormat: 'Y-m-d h:i K',
      monthSelectorType: 'static',
      disableMobile: true,
    });
  });
});
