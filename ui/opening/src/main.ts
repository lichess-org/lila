import LichessReplay from 'replay';

lichess.load.then(() => {
  $('.replay--autoload').each(function (this: HTMLElement) {
    LichessReplay(this, {
      pgn: this.dataset['pgn']!,
      i18n: {},
    });
  });
});
