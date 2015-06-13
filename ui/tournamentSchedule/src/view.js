var m = require('mithril');

module.exports = function(ctrl) {

  return m('div.tournament-schedule', JSON.stringify(ctrl.data));
};
