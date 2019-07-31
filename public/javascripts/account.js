$(function() {

  $('.security table form').submit(function() {
    $.post($(this).attr('action'));
    $(this).parent().parent().fadeOut(300, function() { $(this).remove(); });
    return false;
  });

  $('form.autosubmit').each(function() {
    var $form = $(this);
    $form.find('input').change(function() {
      $.ajax({
        ...lidraughts.formAjax($form),
        success: function() {
          $form.find('.saved').fadeIn();
          lidraughts.storage.set('reload-round-tabs', Math.random());
        }
      });
    });
  });
});
