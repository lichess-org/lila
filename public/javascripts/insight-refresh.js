lichess.refreshInsightForm = () => {
  $('form.insight-refresh:not(.armed)')
    .addClass('armed')
    .on('submit', function () {
      fetch(this.action, {
        method: 'post',
        credentials: 'same-origin',
      }).then(lichess.reload);
      $(this).replaceWith($(this).find('.crunching').show());
      return false;
    });
};
lichess.load.then(lichess.refreshInsightForm);
