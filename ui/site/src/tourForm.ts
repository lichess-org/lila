import flatpickr from 'flatpickr';

lichess.load.then(() => {
  const $variant = $('#form3-variant'),
    showPosition = () =>
      $('.form3 .position').toggleClass('none', !['1', 'standard'].includes($variant.val() as string));

  $variant.on('change', showPosition);
  showPosition();

  $('form .conditions a.show').on('click', function (this: HTMLAnchorElement) {
    $(this).remove();
    $('form .conditions').addClass('visible');
  });

  $('.flatpickr').each(function (this: HTMLInputElement) {
    flatpickr(this, {
      minDate: 'today',
      maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 3),
      dateFormat: 'Z',
      altInput: true,
      altFormat: 'Y-m-d h:i K',
      monthSelectorType: 'static',
      disableMobile: true,
    });
  });
});
