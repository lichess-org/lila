$(function () {
  $('.security table form').submit(function () {
    $.post($(this).attr('action'));
    $(this)
      .parent()
      .parent()
      .fadeOut(300, function () {
        $(this).remove();
      });
    return false;
  });
  var smStorage = lishogi.storage.make('scrollMoves');

  $('form.autosubmit').each(function () {
    var $form = $(this);
    $form.find('input').on('change', function () {
      if (this.name == 'behavior.scrollMoves') {
        smStorage.set(this.value);
        $form.find('.saved').fadeIn();
      }
      const cfg = lishogi.formAjax($form);
      cfg.success = function () {
        $form.find('.saved').fadeIn();
        lishogi.storage.fire('reload-round-tabs');
      };
      $.ajax(cfg);
    });
  });
  $(`#irbehavior_scrollMoves_${smStorage.get() || 1}`).prop('checked', true);
});
