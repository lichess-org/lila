var throttle = require('common').throttle;

module.exports = lichess.sound ? {
  move: throttle(50, false, lichess.sound.move),
  capture: throttle(50, false, lichess.sound.capture),
  check: throttle(50, false, lichess.sound.check)
} : {
  move: $.noop,
  capture: $.noop,
  check: $.noop
};
