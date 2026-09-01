import { h, type VNode } from 'snabbdom';

import { icons } from 'lib/icons';
import { bind, confirm, snabIcon } from 'lib/view';

import type MsgCtrl from '../ctrl';
import type { Convo } from '../interfaces';

export default function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  if (convo.user.id === 'lichess') return [];
  const nodes = [];
  const cls = 'msg-app__convo__action.button.button-empty';
  nodes.push(
    h(
      `a.${cls}.play`,
      {
        key: 'play',
        attrs: { href: `/?user=${convo.user.name}#friend`, title: i18n.challenge.challengeToPlay },
      },
      [snabIcon(icons.Swords)],
    ),
    h('div.msg-app__convo__action__sep', '|'),
  );
  if (convo.relations.out === false)
    nodes.push(
      h(
        `button.${cls}.text.hover-text`,
        {
          key: 'unblock',
          attrs: { title: i18n.site.blocked, type: 'button', 'data-hover-text': i18n.site.unblock },
          hook: bind('click', ctrl.unblock),
        },
        [snabIcon(icons.NotAllowed)],
      ),
    );
  else
    nodes.push(
      h(
        `button.${cls}.bad`,
        {
          key: 'block',
          attrs: { type: 'button', title: i18n.site.block },
          hook: bind('click', withConfirm(ctrl.block)),
        },
        [snabIcon(icons.NotAllowed)],
      ),
    );
  nodes.push(
    h(
      `button.${cls}.bad`,
      {
        key: 'delete',
        attrs: { type: 'button', title: i18n.site.delete },
        hook: bind('click', withConfirm(ctrl.delete)),
      },
      [snabIcon(icons.Trash)],
    ),
    h(
      `a.${cls}.bad`,
      {
        key: 'report',
        attrs: {
          href: '/report/inbox/' + convo.user.name,
          title: i18n.site.reportXToModerators(convo.user.name),
        },
      },
      [snabIcon(icons.CautionTriangle)],
    ),
  );
  return nodes;
}

const withConfirm = (f: () => void) => (e: MouseEvent) => {
  confirm(`${(e.target as HTMLElement).getAttribute('title') || 'Confirm'}?`).then(yes => yes && f());
};
