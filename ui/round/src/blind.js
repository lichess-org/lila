var throttle = require('common').throttle;
var router = require('game').router;

// #FIXME jQuery crap here

var element;

var reload = throttle(1000, false, function(ctrl) {
  var url = (ctrl.data.player.spectator ?
    router.game(ctrl.data, ctrl.data.player.color) :
    router.player(ctrl.data)
  ) + '/text';
  $.ajax({
    url: url,
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
});

module.exports = {
  reload: reload,
  init: function(el, ctrl) {
    element = el;
    reload(ctrl);
  }
};
