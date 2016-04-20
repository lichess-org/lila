var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(ctrl) {

  return m('div.members',
    ctrl.orderedMembers().map(function(member) {
      return m('div', {
        class: (ctrl.vm.follow === member.user.id ? 'following' : '')
      }, [
        m('span.ulpt', {
          'data-href': '/@/' + member.user.name
        }, member.user.name),
        member.user.id === ctrl.userId ? null : m('span.follow.hint--top', {
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
