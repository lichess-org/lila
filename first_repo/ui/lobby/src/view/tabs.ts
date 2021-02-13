import { h } from 'snabbdom';
import { bind } from './util';
import LobbyController from '../ctrl';
import { MaybeVNodes, Tab } from '../interfaces';

function tab(ctrl: LobbyController, key: Tab, active: Tab, content: MaybeVNodes) {
  return h(
    'span',
    {
      class: {
        active: key === active,
        glowing: key !== active && key === 'pools' && !!ctrl.poolMember,
      },
      hook: bind('mousedown', _ => ctrl.setTab(key), ctrl.redraw),
    },
    content
  );
}

export default function (ctrl: LobbyController) {
  const myTurnPovsNb = ctrl.data.nowPlaying.filter(function (p) {
    return p.isMyTurn;
  }).length;
  const active = ctrl.tab;
  return [
    ctrl.isBot ? undefined : tab(ctrl, 'pools', active, [ctrl.trans.noarg('quickPairing')]),
    ctrl.isBot ? undefined : tab(ctrl, 'real_time', active, [ctrl.trans.noarg('lobby')]),
    ctrl.isBot ? undefined : tab(ctrl, 'seeks', active, [ctrl.trans.noarg('correspondence')]),
    active === 'now_playing' || ctrl.data.nbNowPlaying > 0 || ctrl.isBot
      ? tab(ctrl, 'now_playing', active, [
          ctrl.trans.plural('nbGamesInPlay', ctrl.data.nbNowPlaying),
          myTurnPovsNb > 0 ? h('i.unread', myTurnPovsNb) : null,
        ])
      : null,
  ];
}
