import { h } from 'snabbdom';

import { propWithEffect, type Prop } from 'lib';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { storedSet, type StoredSet } from 'lib/storage';
import { bind, dataIcon, onInsert, snabDialog, type Dialog } from 'lib/view';
import { userComplete } from 'lib/view/userComplete';

import type { AnalyseSocketSend } from '../socket';
import { titleNameToId } from '../view/util';
import type { StudyMemberMap } from './interfaces';

export interface StudyInviteFormCtrl {
  members: Prop<StudyMemberMap>;
  spectators: Prop<string[]>;
  show(): void;
  invite(titleName: string): void;
  redraw(): void;
  previouslyInvited: StoredSet<string>;
}

export function makeCtrl(
  send: AnalyseSocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
): StudyInviteFormCtrl {
  let dialog: Dialog | undefined;
  const redraw = () => dialog?.redraw();
  const spectators = propWithEffect<string[]>([], redraw);
  pubsub.on('analysis.closeAll', () => dialog?.close());

  const previouslyInvited = storedSet<string>('study.previouslyInvited', 10);
  const ctrl: StudyInviteFormCtrl = {
    members,
    spectators,
    async show() {
      pubsub.emit('analysis.closeAll');
      dialog = await snabDialog({
        class: 'study__invite study-modal',
        show: true,
        modal: true,
        noScrollable: true,
        easyClose: 'clickOutside',
        render: () => view(ctrl),
      });
    },
    invite(titleName: string) {
      const userId = titleNameToId(titleName);
      send('invite', userId);
      setTimeout(() => previouslyInvited(userId), 1000);
      setTab();
    },
    redraw,
    previouslyInvited,
  };
  return ctrl;
}

function view(ctrl: StudyInviteFormCtrl) {
  const candidates = [...new Set([...ctrl.spectators(), ...ctrl.previouslyInvited()])]
    .filter(s => !ctrl.members()[titleNameToId(s)]) // remove existing members
    .sort();
  return [
    h('h2', i18n.study.inviteToTheStudy),
    h('p.info', { attrs: dataIcon(licon.InfoCircle) }, i18n.study.pleaseOnlyInvitePeopleYouKnow),
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
          candidates.map((username: string) =>
            h(
              'span.button.button-metal',
              { key: username, hook: bind('click', () => ctrl.invite(username)) },
              username,
            ),
          ),
        )
      : undefined,
  ];
}
