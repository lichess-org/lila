import { h } from 'snabbdom';
import RacerCtrl from '../ctrl';
import { Player, Race } from '../interfaces';

export const renderRace = (ctrl: RacerCtrl) => h('div.racer__race', ctrl.players().map(renderTrack(ctrl.race)));

const renderTrack = (race: Race) => (player: Player, index: number) =>
  h(
    'div.racer__race__track',
    h(
      'div.racer__race__player',
      {
        attrs: {
          style: `padding-left:${(95 * player.moves) / race.moves}%`,
        },
      },
      [h(`div.racer__race__player__car.car-${index}`), h('span.racer__race__player__name', playerLink(player))]
    )
  );

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
