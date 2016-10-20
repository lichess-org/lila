var countDownTimeout;

function doCountDown(targetTime) {
  return function curCounter() {
    var secondsToStart = targetTime - (new Date().getTime() / 1000);

    // always play the 0 sound before completing.
    var bestTick = Math.max(0, Math.round(secondsToStart));
    if (bestTick <= 10) $.sound['countDown' + bestTick]();

    if (bestTick > 0) {
      var nextTick = Math.min(10, bestTick - 1);
      countDownTimeout = setTimeout(curCounter, 1000 *
        Math.min(1.1, Math.max(0.8, (secondsToStart - nextTick))));
    }
  };
}

module.exports = {
  end: function(data) {
    if (!data.me) return;
    if (!data.isRecentlyFinished) return;
    if (!lichess.once('tournament.end.sound.' + data.id)) return;

    var soundKey = 'Other';
    if (data.me.rank < 4) soundKey = '1st';
    else if (data.me.rank < 11) soundKey = '2nd';
    else if (data.me.rank < 21) soundKey = '3rd';

    $.sound['tournament' + soundKey]();
  },
  countDown: function(data) {
    if (!data.me || !data.secondsToStart) {
      if (countDownTimeout) clearTimeout(countDownTimeout);
      countDownTimeout = null;
      return;
    }
    if (countDownTimeout) return;
    if (data.secondsToStart > 60 * 60 * 24) return;

    countDownTimeout = setTimeout(
      doCountDown(new Date().getTime() / 1000 + data.secondsToStart - 0.1),
      900);  // wait 900ms before starting countdown.

    // Preload countdown sounds.
    for (var i = 10; i>=0; i--) {
      $.sound.load('countDown' + i);
    }
  }
};
