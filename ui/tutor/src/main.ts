import Lpv from 'lichess-pgn-viewer';

lichess.load.then(() => {
  const eta = $('.tutor__queued').data('eta');
  if (eta) setTimeout(lichess.reload, eta);

  $('.tutor-card--link').on('click', function (this: HTMLElement) {
    const href = this.dataset['href'];
    if (href) lichess.redirect(href);
  });

  $('.tutor__opening .lpv').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: false,
      showClocks: false,
      showPlayers: false,
      chessground: { coordinates: false },
      menu: {
        getPgn: {
          enabled: true,
          fileName: (this.dataset['title'] || this.dataset['pgn'] || 'opening').replace(' ', '_') + '.pgn',
        },
      },
    });
  });
});
