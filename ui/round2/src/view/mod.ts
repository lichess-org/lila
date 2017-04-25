import { Player } from 'game';

import { game } from 'game';

export function blursOf(data, player) {
  return player.blurs ? m('p', [
    player.color,
    ' ' + player.blurs.nb + '/' + game.nbMoves(data, player.color) + ' blurs = ',
    m('strong', player.blurs.percent + '%')
  ]) : null;
}

export function holdOf(player: Player): Mithril.Renderable {
  var h = player.hold;
  return h ? m('p', [
    player.color,
    ' hold alert',
    m('br'),
    'ply=' + h.ply + ', mean=' + h.mean + ' ms, SD=' + h.sd
  ]) : null;
}
