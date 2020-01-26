import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo } from '../interfaces'
import { bind } from './util';
import MsgCtrl from '../ctrl';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(`a.${cls}`, {
      key: 'play',
      attrs: {
        'data-icon': 'U',
        href: `/?user=${convo.user.name}#friend`,
        title: ctrl.trans.noarg('challengeToPlay')
      }
    })
  );
  if (convo.relations.out === false) nodes.push(
    h(`button.${cls}.text.hover-text`, {
      key: 'unblock',
      attrs: {
        'data-icon': 'k',
        title: ctrl.trans.noarg('blocked'),
        'data-hover-text': ctrl.trans.noarg('unblock')
      },
      hook: bind('click', ctrl.unblock)
    })
  );
  else nodes.push(
    h(`button.${cls}.bad`, {
      key: 'block',
      attrs: {
        'data-icon': 'k',
        title: ctrl.trans.noarg('block')
      },
      hook: bind('click', withConfirm(ctrl.block))
    })
  );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'delete',
      attrs: {
        'data-icon': 'q',
        title: ctrl.trans.noarg('delete')
      },
      hook: bind('click', withConfirm(ctrl.delete))
    })
  );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'report',
      attrs: {
        'data-icon': '!',
        href: '/report/flag',
        title: ctrl.trans.noarg('report')
      }
    })
  );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  if (confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`)) f();
}
