import * as xhr from 'common/xhr';

lichess.load.then(() => {
  const arrowSnapStore = lichess.storage.make('arrow.snap');
  const courtesyStore = lichess.storage.make('courtesy');

  $('.security table form').on('submit', function (this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post' });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').show();
    $form.find('input').on('change', function (this: HTMLInputElement) {
      if (this.name == 'behavior.arrowSnap') {
        arrowSnapStore.set(this.value);
        showSaved();
      } else if (this.name == 'behavior.courtesy') {
        courtesyStore.set(this.value);
        showSaved();
      }
      xhr.formToXhr(form).then(() => {
        showSaved();
        lichess.storage.fire('reload-round-tabs');
      });
    });
  });

  $(`#irbehavior_arrowSnap_${arrowSnapStore.get() || 1}`).prop('checked', true);
  $(`#irbehavior_courtesy_${courtesyStore.get() || 0}`).prop('checked', true);
});
