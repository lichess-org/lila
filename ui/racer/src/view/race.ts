import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import RacerCtrl from '../ctrl';
import { Player } from '../interfaces';

export const renderRace = (ctrl: RacerCtrl) => h('div.racer__race', ctrl.players().map(renderPlayer));

const renderPlayer = (player: Player, index: number) =>
  h('div.racer__race__player', [
    h('div.racer__race__player__name', playerLink(player)),
    h(`div.racer__race__player__car.car-${index}`, [player.moves]),
  ]);

export const playerLink = (player: Player) =>
  player.userId
    ? h(
        'a.user-link.ulpt',
        {
          attrs: { href: '/@/' + player.name },
        },
        player.title ? [h('span.utitle', player.title), player.name] : [player.name]
      )
    : h('anonymous', player.name);
