var countDownTimeout;

function doCountDown(seconds) {
  console.log(seconds);
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

    var storageKey = 'tournament.end.sound.' + data.id;
    if (lichess.storage.get(storageKey)) return;
    lichess.storage.set(storageKey, 1);

    var suffixes = ['1st', '2nd', '3rd'];
    var soundKey = 'tournament' + (suffixes[data.me.rank - 1] || 'Other');
    $.sound[soundKey]();
  },
  countDown: function(data) {
    if (!data.me || !data.secondsToStart) {
      if (countDownTimeout) clearTimeout(countDownTimeout);
      countDownTimeout = null;
      return;
    }
    if (countDownTimeout) return;
    countDownTimeout = setTimeout(
      doCountDown(Math.min(data.secondsToStart, 10)), (data.secondsToStart - 10) * 1000);
  }
};
