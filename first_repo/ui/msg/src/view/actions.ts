import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { Convo } from '../interfaces';
import { bind } from './util';
import MsgCtrl from '../ctrl';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  if (convo.user.id == 'lichess') return [];
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(`a.${cls}.play`, {
      key: 'play',
      attrs: {
        'data-icon': 'U',
        href: `/?user=${convo.user.name}#friend`,
        title: ctrl.trans.noarg('challengeToPlay'),
      },
    })
  );
  nodes.push(h('div.msg-app__convo__action__sep', '|'));
  if (convo.relations.out === false)
    nodes.push(
      h(`button.${cls}.text.hover-text`, {
        key: 'unblock',
        attrs: {
          'data-icon': 'k',
          title: ctrl.trans.noarg('blocked'),
          'data-hover-text': ctrl.trans.noarg('unblock'),
        },
        hook: bind('click', ctrl.unblock),
      })
    );
  else
    nodes.push(
      h(`button.${cls}.bad`, {
        key: 'block',
        attrs: {
          'data-icon': 'k',
          title: ctrl.trans.noarg('block'),
        },
        hook: bind('click', withConfirm(ctrl.block)),
      })
    );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'delete',
      attrs: {
        'data-icon': 'q',
        title: ctrl.trans.noarg('delete'),
      },
      hook: bind('click', withConfirm(ctrl.delete)),
    })
  );
  if (!!convo.msgs[0])
    nodes.push(
      h(`button.${cls}.bad`, {
        key: 'report',
        attrs: {
          'data-icon': '!',
          title: ctrl.trans('reportXToModerators', convo.user.name),
        },
        hook: bind('click', withConfirm(ctrl.report)),
      })
    );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  if (confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`)) f();
};
