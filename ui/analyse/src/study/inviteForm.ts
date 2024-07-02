import * as licon from 'common/licon';
import { bind, onInsert } from 'common/snabbdom';
import { titleNameToId } from '../view/util';
import { h, VNode } from 'snabbdom';
import { prop, Prop } from 'common';
import { StudyMemberMap } from './interfaces';
import { AnalyseSocketSend } from '../socket';
import { storedSet, StoredSet } from 'common/storage';
import { snabDialog } from 'common/dialog';

export interface StudyInviteFormCtrl {
  open: Prop<boolean>;
  members: Prop<StudyMemberMap>;
  spectators: Prop<string[]>;
  toggle(): void;
  invite(titleName: string): void;
  redraw(): void;
  trans: Trans;
  previouslyInvited: StoredSet<string>;
}

export function makeCtrl(
  send: AnalyseSocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
  trans: Trans,
): StudyInviteFormCtrl {
  const open = prop(false),
    spectators = prop<string[]>([]);

  const toggle = () => {
    if (!open()) site.pubsub.emit('analyse.close-all');
    open(!open());
    redraw();
  };

  site.pubsub.on('analyse.close-all', () => open(false));

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
    trans,
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
    noScrollable: true,
    vnodes: [
      h('h2', ctrl.trans.noarg('inviteToTheStudy')),
      h(
        'p.info',
        { attrs: { 'data-icon': licon.InfoCircle } },
        ctrl.trans.noarg('pleaseOnlyInvitePeopleYouKnow'),
      ),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername'), spellcheck: 'false' },
          hook: onInsert<HTMLInputElement>(input =>
            site.asset
              .userComplete({
                input,
                tag: 'span',
                onSelect(v) {
                  input.value = '';
                  ctrl.invite(v.name);
                  ctrl.redraw();
                },
              })
              .then(() => input.focus()),
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
