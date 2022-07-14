import LichessReplay from 'replay';

lichess.load.then(() => {
  $('.pgn-replay').each(function (this: HTMLElement) {
    LichessReplay(this, {
      pgn: this.dataset['pgn']!,
      i18n: {},
    });
  });
});
