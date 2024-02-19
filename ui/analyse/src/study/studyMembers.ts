import { AnalyseSocketSend } from '../socket';
import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { iconTag, bind, onInsert, dataIcon, bindNonPassive, looseH as h } from 'common/snabbdom';
import { makeCtrl as inviteFormCtrl, StudyInviteFormCtrl } from './inviteForm';
import { NotifCtrl } from './notif';
import { prop, Prop, scrollTo } from 'common';
import { titleNameToId } from '../view/util';
import { StudyMember, StudyMemberMap, Tab } from './interfaces';
import { textRaw as xhrTextRaw } from 'common/xhr';
import { userLink } from 'common/userLink';
import StudyCtrl from './studyCtrl';

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

export class StudyMemberCtrl {
  dict: Prop<StudyMemberMap>;
  confing = prop<string | null>(null);
  inviteForm: StudyInviteFormCtrl;
  readonly active: Map<string, () => void> = new Map();
  online: { [id: string]: boolean } = {};
  spectatorIds: string[] = [];
  max = 30;

  constructor(readonly opts: Opts) {
    this.dict = prop<StudyMemberMap>(opts.initDict);
    this.inviteForm = inviteFormCtrl(
      opts.send,
      this.dict,
      () => opts.tab('members'),
      opts.redraw,
      opts.trans,
    );
    site.pubsub.on('socket.in.crowd', d => {
      const names: string[] = d.users || [];
      this.inviteForm.spectators(names);
      this.spectatorIds = names.map(titleNameToId);
      this.updateOnline();
    });
  }

  owner = () => this.dict()[this.opts.ownerId];

  isOwner = () => this.opts.myId === this.opts.ownerId || (this.opts.admin && this.canContribute());

  myMember = () => (this.opts.myId ? this.dict()[this.opts.myId] : undefined);

  canContribute = (): boolean => this.myMember()?.role === 'w';

  setActive = (id: string) => {
    if (this.opts.tab() !== 'members') return;
    const active = this.active.get(id);
    if (active) active();
    else
      this.active.set(
        id,
        memberActivity(() => {
          this.active.delete(id);
          this.opts.redraw();
        }),
      );
    this.opts.redraw();
  };

  updateOnline = () => {
    this.online = {};
    const members: StudyMemberMap = this.dict();
    this.spectatorIds.forEach(id => {
      if (members[id]) this.online[id] = true;
    });
    if (this.opts.tab() === 'members') this.opts.redraw();
  };

  update = (members: StudyMemberMap) => {
    if (this.isOwner()) this.confing(Object.keys(members).find(sri => !this.dict()[sri]) || null);
    const wasViewer = this.myMember() && !this.canContribute();
    const wasContrib = this.myMember() && this.canContribute();
    this.dict(members);
    if (wasViewer && this.canContribute()) {
      if (site.once('study-tour')) this.opts.startTour();
      this.opts.onBecomingContributor();
      this.opts.notif.set({
        text: this.opts.trans.noarg('youAreNowAContributor'),
        duration: 3000,
      });
    } else if (wasContrib && !this.canContribute())
      this.opts.notif.set({
        text: this.opts.trans.noarg('youAreNowASpectator'),
        duration: 3000,
      });
    this.updateOnline();
  };
  setRole = (userId: string, role: string) => {
    this.setActive(userId);
    this.opts.send('setRole', { userId, role });
    this.confing(null);
  };
  kick = (id: string) => {
    this.opts.send('kick', id);
    this.confing(null);
  };
  leave = () => this.opts.send('leave');
  ordered = () => {
    const d = this.dict();
    return Object.keys(d)
      .map(id => d[id])
      .sort((a, b) => (a.role === 'r' && b.role === 'w' ? 1 : a.role === 'w' && b.role === 'r' ? -1 : 0));
  };
  size = () => Object.keys(this.dict()).length;
  isOnline = (userId: string) => this.online[userId];
  hasOnlineContributor = () => {
    const members = this.dict();
    for (const i in members) if (this.online[i] && members[i].role === 'w') return true;
    return false;
  };
}

export function view(ctrl: StudyCtrl): VNode {
  const members = ctrl.members,
    isOwner = members.isOwner();

  function statusIcon(member: StudyMember) {
    const contrib = member.role === 'w';
    return h(
      'span.status',
      {
        class: {
          contrib,
          active: members.active.has(member.user.id),
          online: members.isOnline(member.user.id),
        },
        attrs: { title: ctrl.trans.noarg(contrib ? 'contributor' : 'spectator') },
      },
      [iconTag(contrib ? licon.User : licon.Eye)],
    );
  }

  function configButton(ctrl: StudyCtrl, member: StudyMember) {
    if (isOwner && (member.user.id !== members.opts.myId || ctrl.data.admin))
      return h('i.act', {
        attrs: dataIcon(licon.Gear),
        hook: bind(
          'click',
          () => members.confing(members.confing() == member.user.id ? null : member.user.id),
          ctrl.redraw,
        ),
      });
    if (!isOwner && member.user.id === members.opts.myId)
      return h('i.act.leave', {
        attrs: { 'data-icon': licon.InternalArrow, title: ctrl.trans.noarg('leaveTheStudy') },
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
        hook: onInsert(el => scrollTo($(el).parent('.study__members')[0] as HTMLElement, el)),
      },
      [
        h('div.role', [
          h('div.switch', [
            h('input.cmn-toggle', {
              attrs: { id: roleId, type: 'checkbox', checked: member.role === 'w' },
              hook: bind(
                'change',
                e => members.setRole(member.user.id, (e.target as HTMLInputElement).checked ? 'w' : 'r'),
                ctrl.redraw,
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
            { attrs: dataIcon(licon.X), hook: bind('click', _ => members.kick(member.user.id), ctrl.redraw) },
            ctrl.trans.noarg('kick'),
          ),
        ),
      ],
    );
  }

  const ordered: StudyMember[] = members.ordered();

  return h('div.study__members', { hook: onInsert(() => site.pubsub.emit('chat.resize')) }, [
    ...ordered
      .map(member => {
        const confing = members.confing() === member.user.id;
        return [
          h('div', { key: member.user.id, class: { editing: !!confing } }, [
            h('div.left', [statusIcon(member), userLink({ ...member.user, line: false })]),
            configButton(ctrl, member),
          ]),
          confing && memberConfig(member),
        ];
      })
      .reduce((a, b) => a.concat(b), []),
    isOwner &&
      ordered.length < members.max &&
      h('div.add', { key: 'add', hook: bind('click', members.inviteForm.toggle) }, [
        h('div.left', [
          h('span.status', iconTag(licon.PlusButton)),
          h('div.user-link', ctrl.trans.noarg('addMembers')),
        ]),
      ]),
    !members.canContribute() &&
      ctrl.data.admin &&
      h(
        'form.admin',
        {
          key: ':admin',
          hook: bindNonPassive('submit', () => {
            xhrTextRaw(`/study/${ctrl.data.id}/admin`, { method: 'post' }).then(() => location.reload());
            return false;
          }),
        },
        [h('button.button.button-red.button-thin', 'Enter as admin')],
      ),
  ]);
}
