var countDownTimeout;

function doCountDown(targetTime) {
  return function curCounter() {
    var timeToStart = targetTime - (new Date().getTime() / 1000);

    // always play the 0 sound before completing.
    var bestTick = Math.max(0, Math.round(timeToStart));
    if (bestTick <= 10) $.sound['countDown' + bestTick]();

    if (bestTick > 0) {
      var timeToNextTick = timeToStart - Math.min(10, bestTick - 1);
      countDownTimeout = setTimeout(curCounter, timeToNextTick * 1000);
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
      doCountDown(new Date().getTime() / 1000 + data.secondsToStart),
      1000);  // wait 1s before starting countdown.
  }
};
