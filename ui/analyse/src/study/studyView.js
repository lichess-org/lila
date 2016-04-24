var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;

module.exports = function(ctrl) {

  var ownage = ctrl.members.isOwner();

  var username = function(member) {
    var u = member.user;
    return m('span.user_link.ulpt', {
      'data-href': '/@/' + u.name
    }, (u.title ? u.title + ' ' : '') + u.name);
  };

  var roleToggle = function(member) {
    return m('span.role.hint--top', {
      'data-hint': member.role === 'r' ? 'Can read' : 'Can write',
      onclick: ownage ? function() {
        ctrl.toggleRole(member.user.id);
      } : $.noop
    }, member.role.toUpperCase());
  };

  var statusIcon = function(member) {
    var contrib = member.role === 'w';
    return m('span', {
      class: classSet({
        contrib: contrib,
        active: ctrl.members.isActive(member.user.id),
        status: true,
        'hint--top': true
      }),
      'data-hint': contrib ? 'Contributor' : 'Viewer',
    }, m('i', {
      'data-icon': contrib ? '' : 'v'
    }));
  };

  var configButton = function(member, confing) {
    if (!ownage || member.user.id === ctrl.members.myId) return null;
    return m('span.action.config', {
      onclick: function(e) {
        ctrl.members.vm.confing = confing ? null : member.user.id;
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
    ctrl.members.ordered().map(function(member) {
      var confing = ctrl.members.vm.confing === member.user.id;
      var attrs = {
        class: classSet({
          member: true,
          confing: confing
        })
      };
      return [
        m('div', attrs, [
          m('div.left', [
            statusIcon(member),
            username(member)
          ]),
          m('div.right', [
            configButton(member, confing)
          ])
        ]),
        confing ? memberConfig(member) : null
      ];
    }),
    ownage ? invite() : null);
};
