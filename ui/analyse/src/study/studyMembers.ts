import { prop, Prop } from 'common';
import { bind, onInsert, dataIcon } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { AnalyseSocketSend } from '../socket';
import { iconTag, scrollTo, titleNameToId } from '../util';
import { StudyCtrl, StudyMember, StudyMemberMap, Tab } from './interfaces';
import { makeCtrl as inviteFormCtrl, StudyInviteFormCtrl } from './inviteForm';
import { NotifCtrl } from './notif';

export interface StudyMemberCtrl {
  dict: Prop<StudyMemberMap>;
  confing: Prop<string | null>;
  myId?: string;
  inviteForm: StudyInviteFormCtrl;
  update(members: StudyMemberMap): void;
  setActive(id: string): void;
  isActive(id: string): boolean;
  owner(): StudyMember;
  myMember(): StudyMember | undefined;
  isOwner(): boolean;
  canContribute(): boolean;
  max: number;
  setRole(id: string, role: string): void;
  kick(id: string): void;
  leave(): void;
  ordered(): StudyMember[];
  size(): number;
  isOnline(userId: string): boolean;
  hasOnlineContributor(): boolean;
}

interface Opts {
  initDict: StudyMemberMap;
  myId: string | undefined;
  ownerId: string;
  send: AnalyseSocketSend;
  tab: Prop<Tab>;
  startTour(): void;
  notif: NotifCtrl;
  onBecomingContributor(): void;
  admin: boolean;
  redraw(): void;
  trans: Trans;
}

function memberActivity(onIdle: () => void) {
  let timeout: Timeout;
  const schedule = () => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();
  return schedule;
}

export function ctrl(opts: Opts): StudyMemberCtrl {
  const dict = prop<StudyMemberMap>(opts.initDict);
  const confing = prop<string | null>(null);
  const active: { [id: string]: () => void } = {};
  let online: { [id: string]: boolean } = {};
  let spectatorIds: string[] = [];
  const max = 30;

  function owner() {
    return dict()[opts.ownerId];
  }

  function isOwner() {
    return opts.myId === opts.ownerId || (opts.admin && canContribute());
  }

  function myMember() {
    return opts.myId ? dict()[opts.myId] : undefined;
  }

  function canContribute(): boolean {
    const m = myMember();
    return !!m && m.role === 'w';
  }

  const inviteForm = inviteFormCtrl(opts.send, dict, () => opts.tab('members'), opts.redraw, opts.trans);

  function setActive(id: string) {
    if (opts.tab() !== 'members') return;
    if (active[id]) active[id]();
    else
      active[id] = memberActivity(() => {
        delete active[id];
        opts.redraw();
      });
    opts.redraw();
  }

  function updateOnline() {
    online = {};
    const members: StudyMemberMap = dict();
    spectatorIds.forEach(function (id) {
      if (members[id]) online[id] = true;
    });
    if (opts.tab() === 'members') opts.redraw();
  }

  lichess.pubsub.on('socket.in.crowd', d => {
    const names: string[] = d.users || [];
    inviteForm.spectators(names);
    spectatorIds = names.map(titleNameToId);
    updateOnline();
  });

  return {
    dict,
    confing,
    myId: opts.myId,
    inviteForm,
    update(members: StudyMemberMap) {
      if (isOwner()) confing(Object.keys(members).find(sri => !dict()[sri]) || null);
      const wasViewer = myMember() && !canContribute();
      const wasContrib = myMember() && canContribute();
      dict(members);
      if (wasViewer && canContribute()) {
        if (lichess.once('study-tour')) opts.startTour();
        opts.onBecomingContributor();
        opts.notif.set({
          text: opts.trans.noarg('youAreNowAContributor'),
          duration: 3000,
        });
      } else if (wasContrib && !canContribute())
        opts.notif.set({
          text: opts.trans.noarg('youAreNowASpectator'),
          duration: 3000,
        });
      updateOnline();
    },
    setActive,
    isActive(id: string) {
      return !!active[id];
    },
    owner,
    myMember,
    isOwner,
    canContribute,
    max,
    setRole(id: string, role: string) {
      setActive(id);
      opts.send('setRole', {
        userId: id,
        role,
      });
      confing(null);
    },
    kick(id: string) {
      opts.send('kick', id);
      confing(null);
    },
    leave() {
      opts.send('leave');
    },
    ordered() {
      const d = dict();
      return Object.keys(d)
        .map(id => d[id])
        .sort((a, b) => (a.role === 'r' && b.role === 'w' ? 1 : a.role === 'w' && b.role === 'r' ? -1 : 0));
    },
    size() {
      return Object.keys(dict()).length;
    },
    isOnline(userId: string) {
      return online[userId];
    },
    hasOnlineContributor() {
      const members = dict();
      for (const i in members) if (online[i] && members[i].role === 'w') return true;
      return false;
    },
  };
}

export function view(ctrl: StudyCtrl): VNode {
  const members = ctrl.members,
    isOwner = members.isOwner();

  function username(member: StudyMember) {
    const u = member.user;
    return h(
      'span.user-link.ulpt',
      {
        attrs: { 'data-href': '/@/' + u.name },
      },
      (u.title ? u.title + ' ' : '') + u.name
    );
  }

  function statusIcon(member: StudyMember) {
    const contrib = member.role === 'w';
    return h(
      'span.status',
      {
        class: {
          contrib,
          active: members.isActive(member.user.id),
          online: members.isOnline(member.user.id),
        },
        attrs: { title: ctrl.trans.noarg(contrib ? 'contributor' : 'spectator') },
      },
      [iconTag(contrib ? '' : '')]
    );
  }

  function configButton(ctrl: StudyCtrl, member: StudyMember) {
    if (isOwner && (member.user.id !== members.myId || ctrl.data.admin))
      return h('i.act', {
        attrs: dataIcon(''),
        hook: bind(
          'click',
          _ => members.confing(members.confing() == member.user.id ? null : member.user.id),
          ctrl.redraw
        ),
      });
    if (!isOwner && member.user.id === members.myId)
      return h('i.act.leave', {
        attrs: {
          'data-icon': '',
          title: ctrl.trans.noarg('leaveTheStudy'),
        },
        hook: bind('click', members.leave, ctrl.redraw),
      });
    return undefined;
  }

  function memberConfig(member: StudyMember): VNode {
    const roleId = 'member-role';
    return h(
      'm-config',
      {
        key: member.user.id + '-config',
        hook: onInsert(el => scrollTo($(el).parent('.members')[0] as HTMLElement, el)),
      },
      [
        h('div.role', [
          h('div.switch', [
            h('input.cmn-toggle', {
              attrs: {
                id: roleId,
                type: 'checkbox',
                checked: member.role === 'w',
              },
              hook: bind(
                'change',
                e => {
                  members.setRole(member.user.id, (e.target as HTMLInputElement).checked ? 'w' : 'r');
                },
                ctrl.redraw
              ),
            }),
            h('label', { attrs: { for: roleId } }),
          ]),
          h('label', { attrs: { for: roleId } }, ctrl.trans.noarg('contributor')),
        ]),
        h(
          'div.kick',
          h(
            'a.button.button-red.button-empty.text',
            {
              attrs: dataIcon(''),
              hook: bind('click', _ => members.kick(member.user.id), ctrl.redraw),
            },
            ctrl.trans.noarg('kick')
          )
        ),
      ]
    );
  }

  const ordered: StudyMember[] = members.ordered();

  return h(
    'div.study__members',
    {
      hook: onInsert(() => lichess.pubsub.emit('chat.resize')),
    },
    [
      ...ordered
        .map(member => {
          const confing = members.confing() === member.user.id;
          return [
            h(
              'div',
              {
                key: member.user.id,
                class: { editing: !!confing },
              },
              [h('div.left', [statusIcon(member), username(member)]), configButton(ctrl, member)]
            ),
            confing ? memberConfig(member) : null,
          ];
        })
        .reduce((a, b) => a.concat(b), []),
      isOwner && ordered.length < members.max
        ? h(
            'div.add',
            {
              key: 'add',
              hook: bind('click', members.inviteForm.toggle, ctrl.redraw),
            },
            [h('div.left', [h('span.status', iconTag('')), h('div.user-link', ctrl.trans.noarg('addMembers'))])]
          )
        : null,
      !members.canContribute() && ctrl.data.admin
        ? h(
            'form.admin',
            {
              key: ':admin',
              attrs: {
                method: 'post',
                action: `/study/${ctrl.data.id}/admin`,
              },
            },
            [
              h(
                'button.button.button-red.button-thin',
                {
                  attrs: { type: 'submit' },
                },
                'Enter as admin'
              ),
            ]
          )
        : null,
    ]
  );
}
