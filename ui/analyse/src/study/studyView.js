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

  var followButton = function(member) {
    if (member.user.id === ctrl.userId) {
      if (member.role === 'w') return m('span.action.follow', m('i', {
        'data-icon': ''
      }));
      return m('span.action.empty');
    }
    if (member.role !== 'w') return m('span.action.empty');
    return m('span.action.follow.available.hint--top', {
      'data-hint': 'Follow/Unfollow',
      onclick: function(e) {
        e.stopPropagation();
        ctrl.follow(member.user.id);
      }
    }, m('i', {
      'data-icon': ''
    }));
  };

  var configButton = function(member, confing) {
    if (!ownage || member.user.id === ctrl.userId) return null;
    return m('span.action.config.available', {
      onclick: function(e) {
        ctrl.vm.memberConfig = confing ? null : member.user.id;
      }
    }, m('i', {
      'data-icon': '%'
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
      (function(id) {
        return m('div.role', [
          m('div.switch', [
            m('input', {
              id: id,
              class: 'cmn-toggle cmn-toggle-round',
              type: 'checkbox',
              checked: member.role === 'w',
              onchange: function(e) {
                ctrl.setRole(member.user.id, e.target.checked ? 'w' : 'r');
              }
            }),
            m('label', {
              'for': id
            })
          ]),
          m('label', {
            'for': id
          }, 'Contributor')
        ]);
      })('member-role'),
      m('div.kick', m('a.button.text[data-icon=L]', {
        onclick: function() {
          if (confirm('Kick ' + member.user.name + ' out of the study?'))
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
      return [
        m('div', attrs, [
          username(member),
          m('div.actions', [
            configButton(member, confing),
            followButton(member)
          ])
        ]),
        confing ? memberConfig(member) : null
      ];
    }),
    ownage ? invite() : null);
};
