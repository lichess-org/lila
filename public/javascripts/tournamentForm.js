$(function() {

  var $variant = $('#form3-variant');
  var $positionStandard = $('.form3 .position-standard');
  var $positionRussian = $('.form3 .position-russian');
  function showPosition() {
    $positionStandard.toggleNone($variant.val() == 1);
    $positionRussian.toggleNone($variant.val() == 11);
  };
  $variant.on('change', showPosition);
  showPosition();

  $('.tour__form .conditions a.show').on('click', function() {
    $(this).remove();
    $('.tour__form .conditions').addClass('visible');
  });

  $(".tour__form .flatpickr").flatpickr({
    minDate: 'today',
    maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
    dateFormat: 'Z',
    altInput: true,
    altFormat: 'Y-m-d h:i K'
  });
});
