var m = require('mithril');

var chessground = require('chessground');

module.exports = function(ctrl, family) {
  return m('div.board',
    chessground.view(ctrl.vm.inspecting.chessground)
  );
}
