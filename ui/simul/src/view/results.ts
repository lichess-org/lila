import { h } from 'snabbdom';
import * as status from 'game/status';
import type { Pairing } from '../interfaces';
import { opposite } from 'chessground/util';
import type SimulCtrl from '../ctrl';

export default function (ctrl: SimulCtrl) {
  return h('div.results', [
    h(
      'div',
      trans(ctrl, i18n.site.nbPlaying, p => p.game.status < status.ids.aborted),
    ),
    h(
      'div',
      trans(ctrl, i18n.site.nbWins, p => p.game.winner === p.hostColor),
    ),
    h(
      'div',
      trans(ctrl, i18n.site.nbDraws, p => p.game.status >= status.ids.mate && !p.game.winner),
    ),
    h(
      'div',
      trans(ctrl, i18n.site.nbLosses, p => p.game.winner === opposite(p.hostColor)),
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

const trans = (ctrl: SimulCtrl, plural: I18nPlural, cond: (pairing: Pairing) => boolean) =>
  splitNumber(plural(ctrl.data.pairings.filter(cond).length));
