import Lpv from 'lichess-pgn-viewer';

lichess.load.then(() => {
  $('.replay--autoload').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
    });
  });
});
