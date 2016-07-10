var countDownTimeout;

function doCountDown(seconds) {
  return function() {
    $.sound['countDown' + seconds]();
    if (seconds > 0) countDownTimeout = setTimeout(
      doCountDown(seconds - 1),
      1000);
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
      doCountDown(Math.min(data.secondsToStart, 10)), (data.secondsToStart - 10) * 1000);
  }
};
