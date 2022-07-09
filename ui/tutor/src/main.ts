lichess.load.then(() => {
  const eta = $('.tutor__queued').data('eta');
  if (eta) setTimeout(lichess.reload, eta);

  $('.tutor-card--link').on('click', function (this: HTMLElement) {
    const href = this.dataset['href'];
    if (href) lichess.redirect(href);
  });
});
