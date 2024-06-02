site.load.then(() => {
  $('form.insight-refresh').on('submit', function (this: HTMLFormElement) {
    fetch(this.action, {
      method: 'post',
      credentials: 'same-origin',
    }).then(site.reload);

    $(this).replaceWith($(this).find('.crunching').removeClass('none'));

    return false;
  });
});
