var throttle = require('lodash-node/modern/functions/throttle');

// #FIXME jQuery crap here

var element;

var reload = throttle(function(ctrl) {
  var route = ctrl.data.player.spectator ? ctrl.router.Round.watcherText(ctrl.data.game.id, ctrl.data.player.color) : ctrl.router.Round.playerText(ctrl.data.game.id + ctrl.data.player.id);
  $(element).load(route.url, function() {
    $(this).find('form').submit(function() {
      var text = $(this).find('.move').val();
      var move = {
        from: text.substring(0, 2),
        to: text.substring(2, 4),
        promotion: text.substring(4, 5)
      };
      ctrl.socket.send("move", move, {
        ackable: true
      });
      return false;
    }).find('.move').focus();
  });
}, 1000);

module.exports = {
  reload: reload,
  init: function(el, ctrl) {
    element = el;
    reload(ctrl);
  }
};
