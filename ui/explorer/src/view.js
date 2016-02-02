var chessground = require('chessground');
var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.explorer', [
    chessground.view(ctrl.chessground)
  ]);
};
