$(function () {
  function updateFormatClass() {
    var $select = $('#form3-format');
    var $form = $('.form3');
    var selectedValue = $select.val();

    $form.removeClass(function (_index, className) {
      return (className.match(/(^|\s)f-\S+/g) || []).join(' ');
    });

    $form.addClass('f-' + selectedValue);
  }
  function updateTimeControlClass() {
    var $select = $('#form3-timeControlSetup_timeControl');
    var $form = $('.form3');
    var selectedValue = $select.val();

    $form.removeClass('f-corres f-rt');
    $form.addClass('f-' + (selectedValue == 1 ? 'rt' : 'corres'));
  }

  updateFormatClass();
  updateTimeControlClass();

  $('#form3-format').on('change', updateFormatClass);
  $('#form3-timeControlSetup_timeControl').on('change', updateTimeControlClass);

  $('.form-fieldset--toggle').each(function () {
    const toggle = () => this.classList.toggle('form-fieldset--toggle-off');
    $(this)
      .children('legend')
      .on('click', toggle)
      .on('keypress', e => e.key == 'Enter' && toggle());
  });

  $('main form .flatpickr').flatpickr({
    minDate: new Date(Date.now() + 1000 * 60),
    maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 3),
    dateFormat: 'Z',
    altInput: true,
    altFormat: 'Y-m-d h:i K',
    disableMobile: true,
  });
});
