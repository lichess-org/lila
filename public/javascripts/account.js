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
        url: $form.attr('action'),
        method: $form.attr('method'),
        data: $form.serialize(),
        success: function() {
          $form.find('.saved').fadeIn();
          lichess.storage.set('reload-round-tabs', Math.random());
        }
      });
    });
  });
});
