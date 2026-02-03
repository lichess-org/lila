import Lpv from '@lichess-org/pgn-viewer';

site.load.then(() => {
  $('.tutor-card--link').on('click', function (this: HTMLElement) {
    const href = this.dataset['href'];
    if (href) site.redirect(href);
  });

  $('.tutor__opening .lpv').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      orientation: this.dataset['orientation'] as Color,
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

  const waitingGames = Array.from($('.tutor__waiting-game')),
    nbWaitingGames = waitingGames.length;
  if (nbWaitingGames) {
    setTimeout(site.reload, 60 * 1000);
    waitingGames.forEach((el: HTMLElement, index: number) => {
      const lpv = Lpv(el, {
        pgn: el.dataset['pgn']!,
        orientation: el.dataset['pov'] as Color,
        showMoves: false,
        showClocks: false,
        showPlayers: true,
        showControls: false,
        chessground: { coordinates: false, animation: { duration: 100 } },
        drawArrows: false,
      });
      for (let i = 5 - index; i > 0; i--) lpv.goTo('next', false);
      const nbMoves = Array.from(lpv.game.moves.mainline()).length;
      const delayBeforeStart = (index * 1000 * 68) / nbWaitingGames - 9000;
      const moveInterval = 270 - nbMoves;
      setTimeout(() => setInterval(() => lpv.goTo('next', false), moveInterval), delayBeforeStart);
    });
  }
});
