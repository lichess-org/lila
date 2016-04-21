var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;

module.exports = function(ctrl) {

  var ownage = ctrl.userId === ctrl.data.ownerId;

  var username = function(member) {
    return m('span.user_link.ulpt', {
      'data-href': '/@/' + member.user.name
    }, member.user.name);
  };

  var roleToggle = function(member) {
    return m('span.role.hint--top', {
      'data-hint': member.role === 'r' ? 'Can read' : 'Can write',
      onclick: ownage ? function() {
        ctrl.toggleRole(member.user.id);
      } : $.noop
    }, member.role.toUpperCase());
  };

  var rightButton = function(member) {
    if (member.user.id === ctrl.userId) return m('span.action.empty');
    if (member.role !== 'w') return m('span.action.empty');
    return m('span.action.follow.hint--top', {
      'data-hint': 'Follow/Unfollow',
      onclick: function(e) {
        e.stopPropagation();
        ctrl.follow(member.user.id);
      }
    }, m('i', {
      'data-icon': 'v'
    }));
  };

  var invite = function() {
    return m('div.invite', [
      m('input', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.userAutocomplete($(el), {
            onSelect: function(v) {
              ctrl.invite(v);
            }
          });
        },
        class: 'add_member',
        placeholder: 'Invite someone'
      })
    ]);
  };

  var memberConfig = function(member) {
    return m('div.config', [
      m('div', 'Contributor'),
      m('div', m('a.button[data-icon=L]', {
        onclick: function() {
          ctrl.kick(member.user.id);
        }
      }, 'Kick from this study'))
    ]);
  };

  return m('div', {
      class: 'members' + (ownage ? ' ownage' : '')
    },
    ctrl.orderedMembers().map(function(member) {
      var confing = ctrl.vm.memberConfig === member.user.id;
      var attrs = {
        class: classSet({
          member: true,
          following: ctrl.vm.follow === member.user.id,
          confing: confing
        })
      };
      if (ownage) attrs.onclick = function() {
        ctrl.vm.memberConfig = confing ? null : member.user.id;
      }
      return [
        m('div', attrs, [
          username(member),
          rightButton(member),
        ]),
        confing ? memberConfig(member) : null
      ];
    }),
    ownage ? invite() : null);
};
