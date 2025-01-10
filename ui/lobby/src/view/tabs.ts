import { type MaybeVNodes, bind } from 'common/snabbdom';
import { i18n, i18nPluralSame } from 'i18n';
import { h } from 'snabbdom';
import type LobbyController from '../ctrl';
import type { Tab } from '../interfaces';

function tab(ctrl: LobbyController, key: Tab, active: Tab, content: MaybeVNodes) {
  return h(
    'span',
    {
      class: {
        active: key === active,
      },
      hook: bind('mousedown', _ => ctrl.setTab(key), ctrl.redraw),
    },
    content,
  );
}

export default function (ctrl: LobbyController): MaybeVNodes {
  const myTurnPovsNb = ctrl.data.nowPlaying.filter(p => p.isMyTurn).length;
  const active = ctrl.tab;
  return [
    ctrl.isBot ? undefined : tab(ctrl, 'presets', active, [i18n('presets')]),
    ctrl.isBot ? undefined : tab(ctrl, 'real_time', active, [i18n('lobby')]),
    ctrl.isBot ? undefined : tab(ctrl, 'seeks', active, [i18n('correspondence')]),
    active === 'now_playing' || ctrl.data.nbNowPlaying > 0 || ctrl.isBot
      ? tab(ctrl, 'now_playing', active, [
          i18nPluralSame('nbGamesInPlay', ctrl.data.nbNowPlaying),
          myTurnPovsNb > 0 ? h('i.unread', myTurnPovsNb) : null,
        ])
      : null,
  ];
}
