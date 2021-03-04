import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import RacerCtrl from '../ctrl';
import { Player } from '../interfaces';

export const renderRace = (ctrl: RacerCtrl) => h('div.racer__race', ctrl.players().map(renderPlayer));

const renderPlayer = (player: Player) =>
  h('div.racer__race__player', [
    h('div.racer__race__player__name', player.user ? userName(player.user) : ['Anon', ' ', player.index]),
    h(`div.racer__race__player__car.car-${player.index}`, [player.moves]),
  ]);

export const userName = (user: LightUser): Array<string | VNode> =>
  user.title
    ? [h('span.utitle', user.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, user.title), ' ', user.name]
    : [user.name];
