$(function () {
  $('form .conditions a.show').on('click', function () {
    $(this).remove();
    $('form .conditions').addClass('visible');
  });

  $('main form .flatpickr').flatpickr({
    minDate: 'today',
    maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 3),
    dateFormat: 'Z',
    altInput: true,
    altFormat: 'Y-m-d h:i K',
    disableMobile: true,
  });
});
