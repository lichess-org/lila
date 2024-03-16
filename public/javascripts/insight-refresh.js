site.refreshInsightForm = () => {
  $('form.insight-refresh:not(.armed)')
    .addClass('armed')
    .on('submit', function () {
      fetch(this.action, {
        method: 'post',
        credentials: 'same-origin',
      }).then(site.reload);
      $(this).replaceWith($(this).find('.crunching').removeClass('none'));
      return false;
    });
};
site.load.then(site.refreshInsightForm);
