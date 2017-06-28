import { h } from 'snabbdom'
import { titleNameToId, bind, dataIcon } from '../util';
import { prop } from 'common';
import { ctrl as inviteFormCtrl } from './inviteForm';
import { SocketSend } from '../interfaces';
import { StudyMember, StudyMemberMap } from './interfaces';
import { NotifController } from './notif';

interface Opts {
  initDict: StudyMemberMap;
  myId?: string;
  ownerId: string;
  send: SocketSend;
  setTab(): void;
  startTour(): void;
  notif: NotifController;
  onBecomingContributor(): void;
  redraw(): void
}

function memberActivity(onIdle) {
  var timeout;
  var schedule = function() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();
  return schedule;
}

export function ctrl(opts: Opts) {

  const dict = prop<StudyMemberMap>(opts.initDict);
  const confing = prop<string | undefined>(undefined);
  let active: { [id: string]: () => void } = {};
  let online: { [id: string]: boolean } = {};
  let spectatorIds: string[] = [];
  const max = 30;

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
    var m = myMember();
    return m && m.role === 'w';
  };

  var inviteForm = inviteFormCtrl(opts.send, dict, opts.setTab, opts.redraw);

  var setActive = function(id) {
    if (active[id]) active[id]();
    else active[id] = memberActivity(function() {
      delete(active[id]);
      opts.redraw();
    });
    opts.redraw();
  };

  var updateOnline = function() {
    online = {};
    const members: StudyMemberMap = dict();
    spectatorIds.forEach(function(id) {
      if (members[id]) online[id] = true;
    });
    opts.redraw();
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
        if (window.lichess.once('study-tour')) opts.startTour();
        opts.onBecomingContributor();
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
      confing(undefined);
    },
    kick: function(id) {
      opts.send("kick", id);
      confing(undefined);
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
        return a.addedAt > b.addedAt ? 1 : -1;
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
}

export function view(ctrl) {

  var isOwner = ctrl.members.isOwner();

  var username = function(member) {
    var u = member.user;
    return h('span.user_link.ulpt', {
      attrs: { 'data-href': '/@/' + u.name }
    }, (u.title ? u.title + ' ' : '') + u.name);
  };

  var statusIcon = function(member) {
    var contrib = member.role === 'w';
    return h('span.status', {
      class: {
        contrib: contrib,
        active: ctrl.members.isActive(member.user.id),
        online: ctrl.members.isOnline(member.user.id)
      },
      attrs: { title: contrib ? 'Contributor' : 'Viewer' },
    }, [
      h('i', {
        attrs: { 'data-icon': contrib ? '' : 'v' }
      })
    ]);
  };

  var configButton = function(ctrl, member) {
    if (isOwner && member.user.id !== ctrl.members.myId)
    return h('i.action.config', {
      key: 'cfg-' + member.user.id,
      attrs: dataIcon('%'),
      hook: bind('click', _ => {
        ctrl.members.confing(ctrl.members.confing() === member.user.id ? null : member.user.id);
      }, ctrl.redraw)
    });
    if (!isOwner && member.user.id === ctrl.members.myId)
      return h('span.action.leave', {
        key: 'leave',
        attrs: {
          'data-icon': 'F',
          title: 'Leave the study'
        },
        config: bind('click', ctrl.members.leave, ctrl.redraw)
      });
  };

  function memberConfig(member: StudyMember) {
    const roleId = 'member-role';
    return h('div.config', {
      key: member.user.id + '-config',
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).parent('.members').scrollTo(vnode.elm as HTMLElement, 200);
        }
      }
    }, [
      h('div.role', [
        h('div.switch', [
          h('input.cmn-toggle.cmn-toggle-round', {
            attrs: {
              id: roleId,
              type: 'checkbox',
              checked: member.role === 'w'
            },
            hook: bind('change', e => {
              ctrl.members.setRole(member.user.id, (e.target as HTMLInputElement).checked ? 'w' : 'r');
            }, ctrl.redraw)
          }),
          h('label', { attrs: { 'for': roleId } })
        ]),
        h('label', { attrs: { 'for': roleId } }, 'Contributor')
      ]),
      h('div.kick', h('a.button.text', {
        attrs: dataIcon('L'),
        hook: bind('click', _ => ctrl.members.kick(member.user.id), ctrl.redraw)
      }, 'Kick from this study'))
    ]);
  };

  var ordered = ctrl.members.ordered();

  return [
    h('div.list.members', {
      hook: {
        insert: _ => window.lichess.pubsub.emit('content_loaded')()
      }
    }, [
      ordered.map(function(member) {
        const confing = ctrl.members.confing() === member.user.id;
        return [
          h('div.elem.member', {
            key: member.user.id,
            class: { editing: !!confing }
          }, [
            h('div.left', [
              statusIcon(member),
              username(member)
            ]),
            configButton(ctrl, member)
          ]),
          confing ? memberConfig(member) : null
        ];
      }),
      (isOwner && ordered.length < ctrl.members.max) ? h('div.elem.member.add', {
        key: 'invite-someone',
        hook: bind('click', ctrl.members.inviteForm.toggle, ctrl.redraw)
      }, [
        h('div.left', [
          h('span.status', h('i', { attrs: dataIcon('O') })),
          h('span.add_text', 'Add members')
        ])
      ]) : null
    ])
  ];
}
