var throttle = require('./util').throttle;

// #FIXME jQuery crap here

var element;

var reload = throttle(function(ctrl) {
  var route = ctrl.data.player.spectator ? ctrl.router.Round.watcherText(ctrl.data.game.id, ctrl.data.player.color) : ctrl.router.Round.playerText(ctrl.data.game.id + ctrl.data.player.id);
  $.ajax({
    url: route.url,
    cache: false,
    success: function(html) {
      $(element).html(html).find('form').submit(function() {
        var text = $(this).find('.move').val();
        var move = {
          from: text.substr(0, 2),
          to: text.substr(2, 2),
          promotion: text.substr(4, 1)
        };
        ctrl.socket.send("move", move, {
          ackable: true
        });
        return false;
      }).find('.move').focus();
    }
  });
}, 1000);

module.exports = {
  reload: reload,
  init: function(el, ctrl) {
    element = el;
    reload(ctrl);
  }
};
