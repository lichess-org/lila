import { myUserId } from 'lib';
import { licon } from 'lib/licon';
import { bind, type LooseVNodes, hl } from 'lib/view';

import type LobbyController from '@/ctrl';
import type { Tab } from '@/interfaces';

function tab(ctrl: LobbyController, key: Tab, active: Tab, content: LooseVNodes) {
  return hl(
    'button',
    {
      attrs: { role: 'tab' },
      class: { active: key === active, glowing: key !== active && key === 'shortcuts' && !!ctrl.poolMember },
      hook: bind('click', _ => ctrl.setTab(key)),
    },
    content,
  );
}

export default function (ctrl: LobbyController) {
  const nbPlaying = ctrl.data.nbNowPlaying,
    nbMyTurn = ctrl.data.nbMyTurn,
    active = ctrl.tab,
    isBot = ctrl.me?.isBot;
  return [
    !isBot &&
      tab(ctrl, 'shortcuts', active, [
        'Shortcuts',
        myUserId() &&
          hl('i.edit-shortcuts', {
            attrs: { 'data-icon': licon.StarOutline, tabindex: '0', role: 'button', title: i18n.site.edit },
            on: {
              click: e => {
                e.stopPropagation();
                site.asset
                  .loadEsm('lobby.shortcutsDialog', { init: { ctrl: ctrl.shortcutsCtrl } })
                  .then(() => ctrl.redraw());
              },
              keydown: e => {
                if (e.target instanceof HTMLElement && (e.key === 'Enter' || e.key === ' ')) {
                  e.preventDefault();
                  e.target.click();
                }
              },
            },
          }),
      ]),
    !isBot && tab(ctrl, 'real_time', active, [i18n.site.lobby]),
    !isBot && tab(ctrl, 'seeks', active, [i18n.site.correspondence]),
    (active === 'now_playing' || nbPlaying || isBot) &&
      tab(ctrl, 'now_playing', active, [
        i18n.site.nbGamesInPlay.asArray(nbPlaying, nbPlaying >= 100 ? '99+' : nbPlaying.toString()),
        nbMyTurn > 0 && hl('icon.unread', nbMyTurn >= 100 ? '99+' : nbMyTurn),
      ]),
  ];
}
