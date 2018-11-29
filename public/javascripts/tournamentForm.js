$(function() {

  var $variant = $('#form3-variant');
  var $position = $('.form3 .position');
  function showPosition() {
    $position.toggleNone($variant.val() == 1);
  };
  $variant.on('change', showPosition);
  showPosition();

  $('form.create .conditions a.show').on('click', function() {
    $(this).remove();
    $('form.create .conditions').addClass('visible');
  });

  $("form.create .flatpickr").flatpickr({
    minDate: 'today',
    maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
    dateFormat: 'Z',
    altInput: true,
    altFormat: 'Y-m-d h:i K'
  });
});
