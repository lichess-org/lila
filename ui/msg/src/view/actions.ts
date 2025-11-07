import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { bind } from 'lib/snabbdom';
import { confirm } from 'lib/view/dialogs';
import type { Convo } from '../interfaces';
import type MsgCtrl from '../ctrl';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  if (convo.user.id === 'lichess') return [];
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(`a.${cls}.play`, {
      key: 'play',
      attrs: {
        'data-icon': licon.Swords,
        href: `/?user=${convo.user.name}#friend`,
        title: i18n.challenge.challengeToPlay,
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
          title: i18n.site.blocked,
          type: 'button',
          'data-hover-text': i18n.site.unblock,
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
          title: i18n.site.block,
        },
        hook: bind('click', withConfirm(ctrl.block)),
      }),
    );
  nodes.push(
    h(`button.${cls}.bad`, {
      key: 'delete',
      attrs: { 'data-icon': licon.Trash, type: 'button', title: i18n.site.delete },
      hook: bind('click', withConfirm(ctrl.delete)),
    }),
  );
  nodes.push(
    h(`a.${cls}.bad`, {
      key: 'report',
      attrs: {
        href: '/report/inbox/' + convo.user.name,
        'data-icon': licon.CautionTriangle,
        title: i18n.site.reportXToModerators(convo.user.name),
      },
    }),
  );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`).then(yes => yes && f());
};
