window.lishogi.ready.then(() => {
  function updateFormatClass() {
    const $select = $('#form3-format');
    const $form = $('.form3');
    const selectedValue = $select.val();

    $form.removeClass('f-arena f-robin f-organized');

    $form.addClass(`f-${selectedValue}`);
  }
  function updateTimeControlClass() {
    const $select = $('#form3-timeControlSetup_timeControl');
    const $form = $('.form3');
    const selectedValue = $select.val();

    $form.removeClass('f-corres f-rt');
    $form.addClass(`f-${selectedValue == 1 ? 'rt' : 'corres'}`);
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

  document.querySelectorAll('main form .flatpickr').forEach(el => {
    window.flatpickr(el, {
      minDate: new Date(Date.now() + 1000 * 60),
      maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 3),
      dateFormat: 'Z',
      altInput: true,
      altFormat: 'Y-m-d h:i K',
      disableMobile: true,
      locale: document.documentElement.lang as any,
    });
  });
});
