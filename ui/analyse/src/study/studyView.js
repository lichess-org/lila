var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {

  var ownage = ctrl.userId === ctrl.data.ownerId;

  return m('div', {
      class: 'members' + (ownage ? ' ownage' : '')
    },
    ctrl.orderedMembers().map(function(member) {
      return m('div', {
        class: (ctrl.vm.follow === member.user.id ? 'following' : '')
      }, [
        m('span.ulpt', {
          'data-href': '/@/' + member.user.name
        }, member.user.name),
        m('span.role.hint--top', {
          'data-hint': member.role === 'r' ? 'Can read' : 'Can write',
          onclick: ownage ? function() {
            ctrl.toggleRole(member.user.id);
          } : $.noop
        }, member.role.toUpperCase()), (member.role !== 'w' || member.user.id === ctrl.userId) ? m('span.follow.empty') : m('span.follow.hint--top', {
          'data-hint': 'Follow/Unfollow',
          onclick: function() {
            ctrl.follow(member.user.id);
          }
        }, m('i', {
          'data-icon': 'v'
        }))
      ])
    }));
};
