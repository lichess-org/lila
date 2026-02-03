import type { AnalyseSocketSend } from '../socket';
import * as licon from 'lib/licon';
import { type VNode, iconTag, bind, onInsert, dataIcon, bindNonPassive, hl, cmnToggleWrap } from 'lib/view';
import { makeCtrl as inviteFormCtrl, type StudyInviteFormCtrl } from './inviteForm';
import type { NotifCtrl } from './notif';
import { prop, type Prop, scrollTo } from 'lib';
import { titleNameToId } from '../view/util';
import type { StudyMember, StudyMemberMap, Tab } from './interfaces';
import { textRaw as xhrTextRaw } from 'lib/xhr';
import { userLink } from 'lib/view/userLink';
import type StudyCtrl from './studyCtrl';
import { once } from 'lib/storage';
import { pubsub } from 'lib/pubsub';

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
    return hl(
      'span.status',
      {
        class: {
          contrib,
          active: members.active.has(member.user.id),
          online: members.isOnline(member.user.id),
        },
        attrs: { title: i18n.study[contrib ? 'contributor' : 'spectator'] },
      },
      [iconTag(contrib ? licon.User : licon.Eye)],
    );
  }

  function configButton(ctrl: StudyCtrl, member: StudyMember) {
    if (isOwner && (member.user.id !== members.opts.myId || ctrl.data.admin))
      return hl('i.act', {
        attrs: dataIcon(licon.Gear),
        hook: bind(
          'click',
          () => members.confing(members.confing() === member.user.id ? null : member.user.id),
          ctrl.redraw,
        ),
      });
    if (!isOwner && member.user.id === members.opts.myId)
      return hl('i.act.leave', {
        attrs: { 'data-icon': licon.InternalArrow, title: i18n.study.leaveTheStudy },
        hook: bind('click', members.leave, ctrl.redraw),
      });
    return undefined;
  }

  function memberConfig(member: StudyMember): VNode {
    const roleId = 'member-role';
    return hl(
      'm-config',
      {
        key: member.user.id + '-config',
        hook: onInsert(el => scrollTo(el.closest('.study-list')!, el)),
      },
      [
        cmnToggleWrap({
          id: roleId,
          name: i18n.study.contributor,
          checked: member.role === 'w',
          change: v => members.setRole(member.user.id, v ? 'w' : 'r'),
          redraw: ctrl.redraw,
        }),
        hl(
          'div.kick',
          hl(
            'a.button.button-red.button-empty.text',
            { attrs: dataIcon(licon.X), hook: bind('click', _ => members.kick(member.user.id), ctrl.redraw) },
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
      ordered
        .map(member => {
          const confing = members.confing() === member.user.id;
          return [
            hl('div', { key: member.user.id, class: { editing: !!confing } }, [
              hl('div.left', [statusIcon(member), userLink({ ...member.user, line: false })]),
              configButton(ctrl, member),
            ]),
            confing && memberConfig(member),
          ];
        })
        .reduce((a, b) => a.concat(b), []),
    ),
    isOwner &&
      ordered.length < members.max &&
      hl('button.add', { key: 'add', hook: bind('click', members.inviteForm.toggle) }, [
        hl('div.left', [
          hl('span.status', iconTag(licon.PlusButton)),
          hl('div.user-link', i18n.study.addMembers),
        ]),
      ]),
    !members.canContribute() &&
      ctrl.data.admin &&
      hl(
        'form.admin',
        {
          key: ':admin',
          hook: bindNonPassive('submit', () => {
            xhrTextRaw(`/study/${ctrl.data.id}/admin`, { method: 'post' }).then(() => location.reload());
            return false;
          }),
        },
        [hl('button.button.button-red.button-thin', 'Enter as admin')],
      ),
  ]);
}
