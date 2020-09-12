import * as xhr from 'common/xhr';

const li = window.lichess

li.load.then(() => {

  const arrowSnapStore = li.storage.make('arrow.snap');
  const courtesyStore = li.storage.make('courtesy');

  $('.security table form').submit(function(this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post' });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function(this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').fadeIn();
    $form.find('input').change(function(this: HTMLInputElement) {
      if (this.name == 'behavior.arrowSnap') {
        arrowSnapStore.set(this.value);
        showSaved();
      }
      else if (this.name == 'behavior.courtesy') {
        courtesyStore.set(this.value);
        showSaved();
      }
      xhr.formToXhr(form).then(() => {
        showSaved();
        li.storage.fire('reload-round-tabs');
      });
    });
  });

  $(`#irbehavior_arrowSnap_${arrowSnapStore.get() || 1}`).prop('checked', true);
  $(`#irbehavior_courtesy_${courtesyStore.get() || 0}`).prop('checked', true);
});
