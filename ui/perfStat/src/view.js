var m = require('mithril');

module.exports = function(ctrl) {
  return m('div.perfStat', [
      'perf stats',
      JSON.stringify(ctrl.data)
  ]);
};
