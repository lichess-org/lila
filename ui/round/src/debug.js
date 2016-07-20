var users = ['thibault', 'crosky'];

module.exports = function(ctrl) {

  var enabled = ctrl.data.clock && users.indexOf(ctrl.userId) !== -1;

  var nbTicks = 0

  if (enabled) setInterval(function() {
    if (ctrl.clock) console.log([
      'ticks=' + nbTicks,
      'running=' + ctrl.isClockRunning(),
      'turns=' + ctrl.data.game.turns,
      'white=' + ctrl.data.clock.white,
      'black=' + ctrl.data.clock.black
    ].join(' '));
  }, 3000);

  var ifEnabled = function(f) {
    return enabled ? f : $.noop;
  }

  return {
    enabled: enabled,
    tick: ifEnabled(function() {
      nbTicks++;
    }),
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
