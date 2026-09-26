import { prop, type Prop, scrollTo } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { once } from 'lib/storage';
import { type VNode, bind, onInsert, dataIcon, bindNonPassive, hl, icon, button } from 'lib/view';
import { cmnToggleWrap } from 'lib/view/cmn-toggle';
import { userLink } from 'lib/view/userLink';
import { textRaw as xhrTextRaw } from 'lib/xhr';

import type { AnalyseSocketSend } from '../socket';
import { titleNameToId } from '../view/util';
import type { StudyMember, StudyMemberMap, Tab } from './interfaces';
import { makeCtrl as inviteFormCtrl, type StudyInviteFormCtrl } from './inviteForm';
import type { NotifCtrl } from './notif';
import type StudyCtrl from './studyCtrl';

interface Opts {
  initDict: StudyMemberMap;
  myId?: UserId;
  ownerId: UserId;
  send: AnalyseSocketSend;
  tab: Prop<Tab>;
  startTour(): void;
  notif: NotifCtrl;
  onBecomingContributor(): void;
  admin: boolean;
  redraw(): void;
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
  config = prop<UserId | null>(null);
  inviteForm: StudyInviteFormCtrl;
  readonly active: Map<UserId, () => void> = new Map();
  online: Record<UserId, boolean> = {};
  spectatorIds: UserId[] = [];
  max = 30;

  constructor(readonly opts: Opts) {
    this.dict = prop(opts.initDict);
    this.inviteForm = inviteFormCtrl(opts.send, this.dict, () => opts.tab('members'), opts.redraw);
    pubsub.on('socket.in.crowd', d => {
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

  setActive = (id: UserId) => {
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
    if (this.isOwner()) this.config(Object.keys(members).find(sri => !this.dict()[sri]) || null);
    const wasViewer = this.myMember() && !this.canContribute();
    const wasContrib = this.myMember() && this.canContribute();
    this.dict(members);
    if (wasViewer && this.canContribute()) {
      if (once('study-tour')) this.opts.startTour();
      this.opts.onBecomingContributor();
      this.opts.notif.set({
        text: i18n.study.youAreNowAContributor,
        duration: 3000,
      });
    } else if (wasContrib && !this.canContribute())
      this.opts.notif.set({
        text: i18n.study.youAreNowASpectator,
        duration: 3000,
      });
    this.updateOnline();
  };
  setRole = (userId: string, role: string) => {
    this.setActive(userId);
    this.opts.send('setRole', { userId, role });
    this.config(null);
  };
  kick = (id: string) => {
    this.opts.send('kick', id);
    this.config(null);
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
  const { members, data } = ctrl;
  const isOwner = members.isOwner();

  function statusIcon({ user, role }: StudyMember) {
    const contrib = role === 'w';
    return hl(
      'span.status',
      {
        class: {
          contrib,
          active: members.active.has(user.id),
          online: members.isOnline(user.id),
        },
        attrs: { title: i18n.study[contrib ? 'contributor' : 'spectator'] },
      },
      icon(contrib ? licon.User : licon.Eye)(),
    );
  }

  function configButton(ctrl: StudyCtrl, { user }: StudyMember) {
    if (isOwner && (user.id !== members.opts.myId || data.admin))
      return button(
        '.act',
        {
          hook: bind(
            'click',
            () => members.config(members.config() === user.id ? null : user.id),
            ctrl.redraw,
          ),
        },
        icon(licon.Gear)(),
      );
    if (!isOwner && user.id === members.opts.myId)
      return button(
        '.act.leave',
        {
          title: i18n.study.leaveTheStudy,
          hook: bind('click', members.leave, ctrl.redraw),
        },
        icon(licon.InternalArrow)(),
      );
    return undefined;
  }

  function memberConfig({ user, role }: StudyMember): VNode {
    return hl(
      'm-config',
      {
        key: user.id + '-config',
        hook: onInsert(el => scrollTo(el.closest('.study-list')!, el)),
      },
      [
        cmnToggleWrap({
          id: 'member-role',
          name: i18n.study.contributor,
          checked: role === 'w',
          change: v => members.setRole(user.id, v ? 'w' : 'r'),
          redraw: ctrl.redraw,
        }),
        hl(
          'div.kick',
          button(
            '.button.button-red.button-empty.text',
            { ...dataIcon(licon.X), hook: bind('click', _ => members.kick(user.id), ctrl.redraw) },
            i18n.study.kick,
          ),
        ),
      ],
    );
  }

  const ordered: StudyMember[] = members.ordered();

  return hl('div.study__members', [
    hl(
      'div.study-list',
      ordered.flatMap(member => {
        const config = members.config() === member.user.id;
        return [
          hl('div', { key: member.user.id, class: { editing: config } }, [
            hl('div.left', [statusIcon(member), userLink({ ...member.user, line: false })]),
            configButton(ctrl, member),
          ]),
          config && memberConfig(member),
        ];
      }),
    ),
    isOwner &&
      ordered.length < members.max &&
      button('.add', { key: 'add', hook: bind('click', members.inviteForm.toggle) }, [
        icon(licon.PlusButton)(),
        hl('h3', i18n.study.addMembers),
      ]),
    !members.canContribute() &&
      data.admin &&
      hl(
        'form.admin',
        {
          key: ':admin',
          hook: bindNonPassive('submit', () => {
            xhrTextRaw(`/study/${data.id}/admin`, { method: 'post' }).then(() => location.reload());
            return false;
          }),
        },
        button('.button.button-red.button-thin', 'Enter as admin'),
      ),
  ]);
}
