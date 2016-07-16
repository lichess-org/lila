var users = ['thibault', 'crosky'];

module.exports = function(ctrl) {

  var enabled = ctrl.data.clock && users.indexOf(ctrl.userId) !== -1;

  var nbTicks = {
    outer: 0,
    out: 0,
    in : 0,
    redraw: 0
  };

  var interval;

  if (enabled) setInterval(function() {
    if (ctrl.clock) console.log([
      'ticks.outer=' + nbTicks.outer,
      'ticks.out=' + nbTicks.out,
      'ticks.in=' + nbTicks.in,
      'ticks.redraw=' + nbTicks.redraw,
      'player=' + ctrl.data.game.player,
      'status=' + ctrl.data.game.status.id,
      'running=' + ctrl.isClockRunning(),
      'turns=' + ctrl.data.game.turns,
      'white=' + ctrl.data.clock.white,
      'black=' + ctrl.data.clock.black,
      'interval=' + interval
    ].join(' '));
  }, 5000);

  var ifEnabled = function(f) {
    return enabled ? f : $.noop;
  }

  return {
    tickOuter: ifEnabled(function() {
      nbTicks.outer++;
    }),
    tickOut: ifEnabled(function() {
      nbTicks.out++;
    }),
    tickIn: ifEnabled(function() {
      nbTicks.in++;
    }),
    tickRedraw: ifEnabled(function() {
      nbTicks.redraw++;
    }),
    saveInterval: function(i) {
      interval = i;
    },
    log: ifEnabled(function(msg) {
      console.log([
        msg,
        'player=' + ctrl.data.game.player,
        'status=' + ctrl.data.game.status.id,
        'running=' + ctrl.isClockRunning(),
        'turns=' + ctrl.data.game.turns,
        'white=' + ctrl.data.clock.white,
        'black=' + ctrl.data.clock.black
      ].join(' '));
    })
  };
};
