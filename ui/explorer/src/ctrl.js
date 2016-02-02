var chessground = require('chessground');
var m = require('mithril');

function fetchApi(ctrl) {
  m.request({
    method: 'GET',
    url: 'http://130.211.90.176/bullet?fen=' + ctrl.chessground.getFen() + ' w KQkq - 0 1'
  }).then(ctrl.api);
}

module.exports = function(cfg) {
  var self = this;

  this.api = m.prop();

  this.chessground = new chessground.controller({
    events: {
      change: function() {
        fetchApi(self);
      }
    }
  });

  fetchApi(self);
};
