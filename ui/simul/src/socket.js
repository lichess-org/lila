var m = require('mithril');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    reload: function(data) {
      ctrl.reload(data);
      m.redraw();
    },
    ceval: function(data) {
      if (ctrl.evals && data && data.id) {
        if (!ctrl.evals.find(function (e) { return e.id === data.id }))
          ctrl.evals.push(data);
        else ctrl.evals = ctrl.evals.map(function(e) {
          return e.id === data.id ? data : e;
        });
        if (data.game) ctrl.data.pairings.forEach(function(p) {
          if (p.game.id === data.id)
            p.game = data.game;
        });
        m.redraw();
      }
    },
    aborted: function() {
      lidraughts.reload();
    },
    hostGame: function(gameId) {
      ctrl.data.host.gameId = gameId;
      m.redraw();
    }
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
};
