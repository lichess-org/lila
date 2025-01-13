import * as licon from 'common/licon';
import { bind, onInsert } from 'common/snabbdom';
import { titleNameToId } from '../view/util';
import { h, type VNode } from 'snabbdom';
import { prop, type Prop } from 'common';
import type { StudyMemberMap } from './interfaces';
import type { AnalyseSocketSend } from '../socket';
import { storedSet, type StoredSet } from 'common/storage';
import { snabDialog } from 'common/dialog';
import { userComplete } from 'common/userComplete';
import { pubsub } from 'common/pubsub';

export interface StudyInviteFormCtrl {
  open: Prop<boolean>;
  members: Prop<StudyMemberMap>;
  spectators: Prop<string[]>;
  toggle(): void;
  invite(titleName: string): void;
  redraw(): void;
  previouslyInvited: StoredSet<string>;
}

export function makeCtrl(
  send: AnalyseSocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
): StudyInviteFormCtrl {
  const open = prop(false),
    spectators = prop<string[]>([]);

  const toggle = () => {
    if (!open()) pubsub.emit('analysis.closeAll');
    open(!open());
    redraw();
  };

  pubsub.on('analysis.closeAll', () => open(false));

  const previouslyInvited = storedSet<string>('study.previouslyInvited', 10);
  return {
    open,
    members,
    spectators,
    toggle,
    invite(titleName: string) {
      const userId = titleNameToId(titleName);
      send('invite', userId);
      setTimeout(() => previouslyInvited(userId), 1000);
      setTab();
    },
    redraw,
    previouslyInvited,
  };
}

export function view(ctrl: ReturnType<typeof makeCtrl>): VNode {
  const candidates = [...new Set([...ctrl.spectators(), ...ctrl.previouslyInvited()])]
    .filter(s => !ctrl.members()[titleNameToId(s)]) // remove existing members
    .sort();
  return snabDialog({
    class: 'study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    modal: true,
    noScrollable: true,
    vnodes: [
      h('h2', i18n.study.inviteToTheStudy),
      h('p.info', { attrs: { 'data-icon': licon.InfoCircle } }, i18n.study.pleaseOnlyInvitePeopleYouKnow),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: i18n.study.searchByUsername, spellcheck: 'false' },
          hook: onInsert<HTMLInputElement>(input =>
            userComplete({
              input,
              focus: true,
              tag: 'span',
              onSelect(v) {
                input.value = '';
                ctrl.invite(v.name);
                ctrl.redraw();
              },
            }),
          ),
        }),
      ]),
      candidates.length
        ? h(
            'div.users',
            candidates.map(function (username: string) {
              return h(
                'span.button.button-metal',
                { key: username, hook: bind('click', () => ctrl.invite(username)) },
                username,
              );
            }),
          )
        : undefined,
    ],
  });
}
