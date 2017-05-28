module.exports = function(data, onFlag) {
  var timePercentDivisor = 0.1 / data.increment;

  function timePercent(color) {
    return Math.max(0, Math.min(100, times[color] * timePercentDivisor));
  }

  var times;

  function update(white, black) {
    times = {
      white: white * 1000,
      black: black * 1000,
      lastUpdate: Date.now()
    };
  };

  update(data.white, data.black);

  function tick(color) {
    var now = Date.now();
    times[color] -= now - times.lastUpdate;
    times.lastUpdate = now;
    if (times[color] <= 0) onFlag();
  };

  function millisOf(color) {
    return Math.max(0, times[color]);
  };

  return {
    data: data,
    timePercent: timePercent,
    millisOf: millisOf,
    update: update,
    tick: tick
  };
}
