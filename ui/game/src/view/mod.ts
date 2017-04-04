import { Ctrl, Player } from '../interfaces';

import * as m from 'mithril';
import * as game from '../game';

export function blursOf(ctrl: Ctrl, player: Player): Mithril.Renderable {
  return player.blurs ? m('p', [
    player.color,
    ' ' + player.blurs.nb + '/' + game.nbMoves(ctrl.data, player.color) + ' blurs = ',
    m('strong', player.blurs.percent + '%')
  ]) : null;
}

export function holdOf(_ctrl: Ctrl, player: Player): Mithril.Renderable {
  var h = player.hold;
  return h ? m('p', [
    player.color,
    ' hold alert',
    m('br'),
    'ply=' + h.ply + ', mean=' + h.mean + ' ms, SD=' + h.sd
  ]) : null;
}
