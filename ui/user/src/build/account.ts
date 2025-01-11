window.lishogi.ready.then(() => {
  $('.security table form').on('submit', function (this: HTMLFormElement) {
    window.lishogi.xhr.formToXhr(this);
    $(this)
      .parent()
      .parent()
      .fadeOut(300, function () {
        $(this).remove();
      });
    return false;
  });
  const smStorage = window.lishogi.storage.make('scrollMoves');

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this;
    const $form = $(form);
    const showSaved = () => $form.find('.saved').removeClass('none');
    $form.find('input').on('change', function (this: HTMLInputElement) {
      if (this.name == 'behavior.scrollMoves') {
        smStorage.set(this.value);
        $form.find('.saved').fadeIn();
      }
      window.lishogi.xhr.formToXhr(form).then(() => {
        showSaved();
        window.lishogi.storage.fire('reload-round-tabs');
      });
    });
  });
  $(`#irbehavior_scrollMoves_${smStorage.get() || 1}`).prop('checked', true);
});
