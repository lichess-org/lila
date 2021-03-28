import { h } from 'snabbdom';
import * as status from 'game/status';
import { Pairing } from '../interfaces';
import { opposite } from 'chessground/util';
import SimulCtrl from '../ctrl';

export default function (ctrl: SimulCtrl) {
  return h('div.results', [
    h(
      'div',
      trans(ctrl, 'nbPlaying', p => p.game.status < status.ids.mate)
    ),
    h(
      'div',
      trans(ctrl, 'nbWins', p => p.game.winner === p.hostColor)
    ),
    h(
      'div',
      trans(ctrl, 'nbDraws', p => p.game.status >= status.ids.mate && !p.game.winner)
    ),
    h(
      'div',
      trans(ctrl, 'nbLosses', p => p.game.winner === opposite(p.hostColor))
    ),
  ]);
}

const NumberFirstRegex = /^(\d+)\s(.+)$/,
  NumberLastRegex = /^(.+)\s(\d+)$/;

const splitNumber = (s: string) => {
  let found: string[] | null;
  if ((found = s.match(NumberFirstRegex))) return [h('div.number', found[1]), h('div.text', found[2])];
  if ((found = s.match(NumberLastRegex))) return [h('div.number', found[2]), h('div.text', found[1])];
  return h('div.text', s);
};

const trans = (ctrl: SimulCtrl, key: string, cond: (pairing: Pairing) => boolean) =>
  splitNumber(ctrl.trans.plural(key, ctrl.data.pairings.filter(cond).length));
