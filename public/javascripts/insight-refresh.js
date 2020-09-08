lichess.refreshInsightForm = function() {
  $('form.insight-refresh:not(.armed)').addClass('armed').submit(function() {
    fetch(this.action, {
      method: 'post',
      credentials: 'same-origin',
    }).then(lichess.reload);
    if (lichess.modal) lichess.modal($(this).find('.crunching'));
    else $(this).replaceWith($(this).find('.crunching').show());
    return false;
  });
}
window.lichess.load.then(lichess.refreshInsightForm);
