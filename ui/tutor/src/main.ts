lichess.load.then(() => {
  const eta = $('.tutor__queued').data('eta');
  if (eta) setTimeout(lichess.reload, eta);
});
