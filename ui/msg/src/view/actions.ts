import { bind } from 'common/snabbdom';
import { i18n, i18nFormat } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type MsgCtrl from '../ctrl';
import type { Convo } from '../interfaces';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  if (convo.user.id == 'lishogi') return [];
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(`a.${cls}.play`, {
      key: 'play',
      attrs: {
        'data-icon': 'U',
        href: `/?user=${convo.user.name}#friend`,
        title: i18n('challengeToPlay'),
      },
    }),
  );
  nodes.push(h('div.msg-app__convo__action__sep', '|'));
  if (convo.relations.out === false)
    nodes.push(
      h(`button.${cls}.text.hover-text`, {
        key: 'unblock',
        attrs: {
          'data-icon': 'k',
          title: i18n('blocked'),
          type: 'button',
          'data-hover-text': i18n('unblock'),
        },
        hook: bind('click', ctrl.unblock),
      }),
    );
  else
    nodes.push(
      h(`button.${cls}.bad`, {
        key: 'block',
        attrs: {
          'data-icon': 'k',
          type: 'button',
          title: i18n('block'),
        },
        hook: bind('click', withConfirm(ctrl.block)),
      }),
    );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'delete',
      attrs: {
        'data-icon': 'q',
        type: 'button',
        title: i18n('delete'),
      },
      hook: bind('click', withConfirm(ctrl.delete)),
    }),
  );
  if (!!convo.msgs[0])
    nodes.push(
      h(`button.${cls}.bad`, {
        key: 'report',
        attrs: {
          'data-icon': '!',
          title: i18nFormat('reportXToModerators', convo.user.name),
        },
        hook: bind('click', withConfirm(ctrl.report)),
      }),
    );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  if (confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`)) f();
};
