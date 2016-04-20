var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {

  return m('div.members',
    ctrl.orderedMembers().map(function(member) {
      return m('div', {
        class: 'ulpt',
        'data-href': '/@/' + member.user.name
      }, member.user.name);
    }));
};
