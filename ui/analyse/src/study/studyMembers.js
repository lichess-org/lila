var m = require('mithril');
var classSet = require('common').classSet;
var bindOnce = require('../util').bindOnce;
var titleNameToId = require('../util').titleNameToId;
var inviteFormCtrl = require('./inviteForm').ctrl;

function memberActivity(onIdle) {
  var timeout;
  var schedule = function() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();
  return schedule;
}

module.exports = {
  ctrl: function(opts) {

    var dict = m.prop(opts.initDict);
    var confing = m.prop(null); // which user is being configured by us
    var active = {}; // recently active contributors
    var online = {}; // userId -> bool
    var spectatorIds = [];
    var max = 30;

    var owner = function() {
      return dict()[opts.ownerId];
    };

    var isOwner = function() {
      return opts.myId === opts.ownerId;
    };

    var myMember = function() {
      return opts.myId ? dict()[opts.myId] : null;
    };

    var canContribute = function() {
      return (myMember() || {}).role === 'w';
    };

    var inviteForm = inviteFormCtrl(opts.send, dict, opts.setTab);

    var setActive = function(id) {
      if (active[id]) active[id]();
      else active[id] = memberActivity(function() {
        delete(active[id]);
        m.redraw();
      });
      m.redraw();
    };

    var updateOnline = function() {
      online = {};
      var members = dict();
      spectatorIds.forEach(function(id) {
        if (members[id]) online[id] = true;
      });
      m.redraw();
    }

    return {
      dict: dict,
      confing: confing,
      myId: opts.myId,
      inviteForm: inviteForm,
      update: function(members) {
        if (isOwner()) confing(Object.keys(members).find(function(uid) {
          return !dict()[uid];
        }));
        var wasViewer = myMember() && !canContribute();
        var wasContrib = myMember() && canContribute();
        dict(members);
        if (wasViewer && canContribute()) {
          if (lichess.once('study-tour')) opts.startTour();
          opts.notif.set({
            text: 'You are now a contributor',
            duration: 3000
          });
        } else if (wasContrib && !canContribute()) opts.notif.set({
          text: 'You are now a spectator',
          duration: 3000
        });
        updateOnline();
      },
      setActive: setActive,
      isActive: function(id) {
        return !!active[id];
      },
      owner: owner,
      myMember: myMember,
      isOwner: isOwner,
      canContribute: canContribute,
      max: max,
      setRole: function(id, role) {
        setActive(id);
        opts.send("setRole", {
          userId: id,
          role: role
        });
        confing(null);
      },
      kick: function(id) {
        opts.send("kick", id);
        confing(null);
      },
      leave: function() {
        opts.send("leave");
      },
      ordered: function() {
        var d = dict();
        return Object.keys(d).map(function(id) {
          return d[id];
        }).sort(function(a, b) {
          if (a.role === 'r' && b.role === 'w') return 1;
          if (a.role === 'w' && b.role === 'r') return -1;
          return a.addedAt > b.addedAt;
        });
      },
      size: function() {
        return Object.keys(dict()).length;
      },
      setSpectators: function(usernames) {
        this.inviteForm.setSpectators(usernames);
        spectatorIds = usernames.map(titleNameToId);
        updateOnline();
      },
      isOnline: function(userId) {
        return online[userId];
      },
      titleNameToId: titleNameToId,
      hasOnlineContributor: function() {
        var members = dict();
        for (var i in members)
          if (online[i] && members[i].role === 'w') return true;
        return false;
      }
    };
  },
  view: function(ctrl) {

    var isOwner = ctrl.members.isOwner();

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
          online: ctrl.members.isOnline(member.user.id),
          status: true
        }),
        title: contrib ? 'Contributor' : 'Viewer',
      }, m('i', {
        'data-icon': contrib ? '' : 'v'
      }));
    };

    var configButton = function(ctrl, member) {
      if (isOwner && member.user.id !== ctrl.members.myId)
        return m('i.action.config', {
          'data-icon': '%',
          key: 'config-' + member.user.id,
          config: bindOnce('click', function() {
            ctrl.members.confing(ctrl.members.confing() === member.user.id ? null : member.user.id);
          })
        });
      if (!isOwner && member.user.id === ctrl.members.myId)
        return m('span.action.leave', {
          'data-icon': 'F',
          key: 'leave',
          title: 'Leave the study',
          config: bindOnce('click', ctrl.members.leave)
        });
    };

    var memberConfig = function(member) {
      return m('div.config', {
        key: member.user.id + '-config',
        config: function(el, isUpdate) {
          if (!isUpdate) $(el).parent('.members').scrollTo(el, 200);
        }
      }, [
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
          onclick: lichess.partial(ctrl.members.kick, member.user.id)
        }, 'Kick from this study'))
      ]);
    };

    var ordered = ctrl.members.ordered();

    return [
      m('div', {
        key: 'members',
        class: 'list members',
        config: function() {
          lichess.pubsub.emit('content_loaded')();
        }
      }, [
        ordered.map(function(member) {
          var confing = ctrl.members.confing() === member.user.id;
          return [
            m('div', {
              key: member.user.id,
              class: 'elem member' + (confing ? ' editing' : '')
            }, [
              m('div.left', [
                statusIcon(member),
                username(member)
              ]),
              configButton(ctrl, member)
            ]),
            confing ? memberConfig(member) : null
          ];
        }),
        (isOwner && ordered.length < ctrl.members.max) ? m('div', {
            key: 'invite-someone',
            class: 'elem member add',
            config: bindOnce('click', ctrl.members.inviteForm.toggle)
          },
          m('div.left', [
            m('span.status', m('i[data-icon=O]')),
            m('span.add_text', 'Add members')
          ])
        ) : null
      ])
    ];
  }
};
