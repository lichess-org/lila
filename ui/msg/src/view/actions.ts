import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { Convo } from '../interfaces';
import MsgCtrl from '../ctrl';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  if (convo.user.id == 'lichess') return [];
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(`a.${cls}.play`, {
      key: 'play',
      attrs: {
        'data-icon': licon.Swords,
        href: `/?user=${convo.user.name}#friend`,
        title: ctrl.trans.noarg('challengeToPlay'),
      },
    }),
  );
  nodes.push(h('div.msg-app__convo__action__sep', '|'));
  if (convo.relations.out === false)
    nodes.push(
      h(`button.${cls}.text.hover-text`, {
        key: 'unblock',
        attrs: {
          'data-icon': licon.NotAllowed,
          title: ctrl.trans.noarg('blocked'),
          type: 'button',
          'data-hover-text': ctrl.trans.noarg('unblock'),
        },
        hook: bind('click', ctrl.unblock),
      }),
    );
  else
    nodes.push(
      h(`button.${cls}.bad`, {
        key: 'block',
        attrs: {
          'data-icon': licon.NotAllowed,
          type: 'button',
          title: ctrl.trans.noarg('block'),
        },
        hook: bind('click', withConfirm(ctrl.block)),
      }),
    );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'delete',
      attrs: { 'data-icon': licon.Trash, type: 'button', title: ctrl.trans.noarg('delete') },
      hook: bind('click', withConfirm(ctrl.delete)),
    }),
  );
  nodes.push(
    h(`a.${cls}.bad`, {
      key: 'report',
      attrs: {
        href: '/report/inbox/' + convo.user.name,
        'data-icon': licon.CautionTriangle,
        title: ctrl.trans('reportXToModerators', convo.user.name),
      },
    }),
  );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  if (confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`)) f();
};
