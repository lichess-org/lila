var m = require('mithril');

function empty() {
  return m('div.empty', lichess.spinnerHtml);
}

function show(data) {
  return m('div.data',
    JSON.stringify(data));
}

module.exports = {
  renderExplorer: function(ctrl) {
    if (!ctrl.explorer.enabled()) return;
    var data = ctrl.explorer.get(ctrl.vm.step.fen);
    return m('div.explorer_box', data ? show(data) : empty());
  }
};
