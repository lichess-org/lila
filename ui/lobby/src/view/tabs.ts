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
  const nbPlaying = ctrl.data.nbNowPlaying;
  const myTurnPovsNb = ctrl.data.nowPlaying.filter(p => p.isMyTurn).length;
  const active = ctrl.tab;
  return [
    ctrl.isBot ? undefined : tab(ctrl, 'pools', active, [ctrl.trans.noarg('quickPairing')]),
    ctrl.isBot ? undefined : tab(ctrl, 'real_time', active, [ctrl.trans.noarg('lobby')]),
    ctrl.isBot ? undefined : tab(ctrl, 'seeks', active, [ctrl.trans.noarg('correspondence')]),
    active === 'now_playing' || nbPlaying || ctrl.isBot
      ? tab(ctrl, 'now_playing', active, [
          ...ctrl.trans.vdomPlural('nbGamesInPlay', nbPlaying, nbPlaying >= 100 ? '100+' : nbPlaying.toString()),
          myTurnPovsNb > 0 ? h('i.unread', myTurnPovsNb >= 9 ? '9+' : myTurnPovsNb) : null,
        ])
      : null,
  ];
}
