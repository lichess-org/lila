import { bind, titleNameToId, onInsert } from '../util';
import { h } from 'snabbdom';
import { modal } from '../modal';
import { prop, Prop } from 'common';
import { StudyMemberMap } from './interfaces';
import { VNode } from 'snabbdom';

export function makeCtrl(
  send: SocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
  trans: Trans
) {
  const open = prop(false),
    spectators = prop<string[]>([]);
  return {
    open,
    members,
    spectators,
    toggle() {
      open(!open());
    },
    invite(titleName: string) {
      send('invite', titleNameToId(titleName));
      setTab();
    },
    redraw,
    trans,
  };
}

export function view(ctrl: ReturnType<typeof makeCtrl>): VNode {
  const candidates = ctrl
    .spectators()
    .filter(s => !ctrl.members()[titleNameToId(s)]) // remove existing members
    .sort();
  return modal({
    class: 'study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg('inviteToTheStudy')),
      h('p.info', { attrs: { 'data-icon': '' } }, ctrl.trans.noarg('pleaseOnlyInvitePeopleYouKnow')),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(input =>
            lichess.userComplete().then(uac => {
              uac({
                input,
                tag: 'span',
                onSelect(v) {
                  input.value = '';
                  ctrl.invite(v.name);
                  ctrl.redraw();
                },
              });
              input.focus();
            })
          ),
        }),
      ]),
      candidates.length
        ? h(
            'div.users',
            candidates.map(function (username: string) {
              return h(
                'span.button.button-metal',
                {
                  key: username,
                  hook: bind('click', _ => ctrl.invite(username)),
                },
                username
              );
            })
          )
        : undefined,
    ],
  });
}
