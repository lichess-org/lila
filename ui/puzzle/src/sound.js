var throttle = require('common').throttle;

module.exports = $.sound ? {
  move: throttle(50, false, $.sound.move),
  capture: throttle(50, false, $.sound.capture),
  check: throttle(50, false, $.sound.check)
} : {
  move: $.noop,
  capture: $.noop,
  check: $.noop
};
