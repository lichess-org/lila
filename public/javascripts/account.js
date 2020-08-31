$(function() {

  const arrowSnapStore = lichess.storage.make('arrow.snap');
  const courtesyStore = lichess.storage.make('courtesy');

  $('.security table form').submit(function() {
    $.post($(this).attr('action'));
    $(this).parent().parent().fadeOut(300, function() { $(this).remove(); });
    return false;
  });

  $('form.autosubmit').each(function() {
    const $form = $(this);
    const showSaved = () => $form.find('.saved').fadeIn();
    $form.find('input').change(function() {
      if (this.name == 'behavior.arrowSnap') {
        arrowSnapStore.set(this.value);
        showSaved();
      }
      else if (this.name == 'behavior.courtesy') {
        courtesyStore.set(this.value);
        showSaved();
      }
      const cfg = lichess.formAjax($form);
      cfg.success = () => {
        showSaved();
        lichess.storage.fire('reload-round-tabs');
      };
      $.ajax(cfg);
    });
  });

  $(`#irbehavior_arrowSnap_${arrowSnapStore.get() || 1}`).prop('checked', true);
  $(`#irbehavior_courtesy_${courtesyStore.get() || 0}`).prop('checked', true);
});
