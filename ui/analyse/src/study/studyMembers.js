var m = require('mithril');
var classSet = require('chessground').util.classSet;
var util = require('../util');
var inviteFormCtrl = require('./inviteForm').ctrl;
var partial = require('chessground').util.partial;

function memberActivity(onIdle) {
  var timeout;
  var schedule = function() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();
  return schedule;
};

module.exports = {
  ctrl: function(initDict, myId, ownerId, send, setTab, startTour, notif) {

    var dict = m.prop(initDict);
    var confing = m.prop(null); // which user is being configured by us
    var active = {}; // recently active contributors
    var online = {}; // userId -> bool
    var spectatorIds = [];

    var owner = function() {
      return dict()[ownerId];
    };

    var isOwner = function() {
      return myId === ownerId;
    };

    var myMember = function() {
      return myId ? dict()[myId] : null;
    };

    var canContribute = function() {
      return (myMember() || {}).role === 'w';
    };

    var inviteForm = inviteFormCtrl(send, dict, setTab);

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
      myId: myId,
      inviteForm: inviteForm,
      update: function(members) {
        if (isOwner()) confing(Object.keys(members).filter(function(uid) {
          return !dict()[uid];
        })[0]);
        var wasViewer = myMember() && !canContribute();
        var wasContrib = myMember() && canContribute();
        dict(members);
        if (wasViewer && canContribute()) {
          if (lichess.once('study-tour')) startTour();
          notif.set({
            text: 'You are now a contributor',
            duration: 3000
          });
        } else if (wasContrib && !canContribute()) notif.set({
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
      setRole: function(id, role) {
        setActive(id);
        send("setRole", {
          userId: id,
          role: role
        });
        confing(null);
      },
      kick: function(id) {
        send("kick", id);
        confing(null);
      },
      leave: function() {
        send("leave");
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
        spectatorIds = usernames.map(util.titleNameToId);
        updateOnline();
      },
      isOnline: function(userId) {
        return online[userId];
      },
      titleNameToId: util.titleNameToId
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
        return m('span.action.config', {
          key: 'config-' + member.user.id,
          config: util.bindOnce('click', function() {
            ctrl.members.confing(ctrl.members.confing() === member.user.id ? null : member.user.id);
          })
        }, m('i', {
          'data-icon': '%'
        }));
      if (!isOwner && member.user.id === ctrl.members.myId)
        return m('span.action.leave', {
          key: 'leave',
          title: 'Leave the study',
          config: util.bindOnce('click', ctrl.members.leave)
        }, m('i', {
          'data-icon': 'F'
        }));
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
          onclick: partial(ctrl.members.kick, member.user.id)
        }, 'Kick from this study'))
      ]);
    };

    return [
      m('div', {
        key: 'members',
        class: 'list members',
        config: function() {
          lichess.pubsub.emit('content_loaded')();
        }
      }, ctrl.members.ordered().map(function(member) {
        var confing = ctrl.members.confing() === member.user.id;
        var attrs = {
          key: member.user.id,
          class: classSet({
            elem: true,
            member: true,
            editing: confing
          })
        };
        return [
          m('div', attrs, [
            m('div.left', [
              statusIcon(member),
              username(member)
            ]),
            configButton(ctrl, member)
          ]),
          confing ? memberConfig(member) : null
        ];
      })),
      isOwner ? m('i.add[data-icon=0]', {
        key: 'new-member',
        title: 'Invite someone',
        'data-icon': 'O',
        config: util.bindOnce('click', ctrl.members.inviteForm.toggle)
      }) : null
    ];
  }
};
