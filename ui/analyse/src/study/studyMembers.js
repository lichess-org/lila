var m = require('mithril');
var classSet = require('chessground').util.classSet;
var inviteFormCtrl = require('./inviteForm').ctrl;

function memberActivity(onIdle) {
  var timeout;
  var schedule = function() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();

  return {
    renew: function() {
      schedule();
    }
  };
};

module.exports = {
  ctrl: function(initMap, myId, ownerId, send, setTab) {

    var map = m.prop(initMap);
    var confing = m.prop(); // which user is being configured by us
    var active = {}; // recently active contributors

    var owner = function() {
      return map()[ownerId];
    };

    var myMember = function() {
      return myId ? map()[myId] : null;
    };

    var inviteForm = inviteFormCtrl(send, map, setTab);

    return {
      map: map,
      confing: confing,
      myId: myId,
      inviteForm: inviteForm,
      setActive: function(id) {
        if (active[id]) active[id].renew();
        else active[id] = memberActivity(function() {
          delete(active[id]);
          m.redraw();
        });
        m.redraw();
      },
      isActive: function(id) {
        return !!active[id];
      },
      owner: owner,
      myMember: myMember,
      isOwner: function() {
        return myId === ownerId;
      },
      canContribute: function() {
        return myMember() && myMember().role === 'w';
      },
      setRole: function(id, role) {
        send("setRole", {
          userId: id,
          role: role
        });
        setTimeout(function() {
          confing(null);
          m.redraw();
        }, 400);
      },
      kick: function(id) {
        send("kick", id);
        confing(null);
      },
      ordered: function() {
        return Object.keys(map()).map(function(id) {
          return map()[id];
        }).sort(function(a, b) {
          if (a.role === 'r' && b.role === 'w') return 1;
          if (a.role === 'w' && b.role === 'r') return -1;
          return a.addedAt > b.addedAt;
        });
      },
    };
  },
  view: function(ctrl) {

    var ownage = ctrl.members.isOwner();

    var username = function(member) {
      var u = member.user;
      return m('span.user_link.ulpt', {
        'data-href': '/@/' + u.name
      }, (u.title ? u.title + ' ' : '') + u.name);
    };

    var statusIcon = function(member) {
      var contrib = member.role === 'w';
      return m('span', {
        class: classSet({
          contrib: contrib,
          active: ctrl.members.isActive(member.user.id),
          status: true
        }),
        title: contrib ? 'Contributor' : 'Viewer',
      }, m('i', {
        'data-icon': contrib ? '' : 'v'
      }));
    };

    var configButton = function(member, confing) {
      if (!ownage || member.user.id === ctrl.members.myId) return null;
      return m('span.action.config', {
        onclick: function() {
          ctrl.members.confing(confing ? null : member.user.id);
        }
      }, m('i', {
        'data-icon': '%'
      }));
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
                  ctrl.members.setRole(member.user.id, e.target.checked ? 'w' : 'r');
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
              ctrl.members.kick(member.user.id);
          }
        }, 'Kick from this study'))
      ]);
    };

    return m('div', {
      class: 'list members' + (ownage ? ' ownage' : ''),
      config: function() {
        $('body').trigger('lichess.content_loaded');
      }
    }, [
      ctrl.members.ordered().map(function(member) {
        var confing = ctrl.members.confing() === member.user.id;
        var attrs = {
          class: classSet({
            elem: true,
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
      })
    ]);
  }
};
