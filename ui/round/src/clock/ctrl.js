var updateElements = require('./view').updateElements;

module.exports = function(data, opts) {
  var emergSound = {
    play: lichess.sound.lowtime,
    next: null,
    delay: 20000,
    playable: {
      white: true,
      black: true
    }
  };

  var timePercentDivisor = .1 / (Math.max(data.initial, 2) + 5 * data.increment);

  function timePercent(color) {
    return Math.max(0, Math.min(100, times[color] * timePercentDivisor));
  }

  var emergMs = 1000 * Math.min(60, Math.max(10, data.initial * .125));

  var times;

  function update(white, black, delayCentis) {
    if (delayCentis === undefined) delayCentis = 1;
    times = {
      white: white * 1000,
      black: black * 1000,
      lastUpdate: Date.now() + delayCentis * 10
    };
  };

  update(data.white, data.black);

  function tick(ctrl, color) {
    var now = Date.now();
    if (now > times.lastUpdate) {
      times[color] -= now - times.lastUpdate;
      times.lastUpdate = now;
    }
    var millis = times[color];

    if (millis <= 0) opts.onFlag();
    else updateElements(ctrl, color);

    if (opts.soundColor == color) {
      if (emergSound.playable[color]) {
        if (millis < emergMs && !(now < emergSound.next)) {
          emergSound.play();
          emergSound.next = now + emergSound.delay;
          emergSound.playable[color] = false;
        }
      } else if (millis > 1.5 * emergMs) {
         emergSound.playable[color] = true;
      }
    }
  };

  function millisOf(color) {
    return Math.max(0, times[color]);
  };

  return {
    data: data,
    timePercent: timePercent,
    update: update,
    tick: tick,
    millisOf: millisOf,
    emergMs: emergMs,
    elements: {
      white: {},
      black: {}
    }
  };
}
