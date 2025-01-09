import { h } from 'snabbdom';
import * as status from 'game/status';
import SimulCtrl from '../ctrl';
import { i18nPluralSame } from 'i18n';

export default function (ctrl: SimulCtrl) {
  return h('div.results', [
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbPlaying',
          ctrl.data.pairings.filter(p => p.game.status < status.ids.aborted).length
        )
      )
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbWins',
          ctrl.data.pairings.filter(p => p.game.winner === p.hostColor).length
        )
      )
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbDraws',
          ctrl.data.pairings.filter(p => p.game.status >= status.ids.mate && !p.game.winner).length
        )
      )
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbLosses',
          ctrl.data.pairings.filter(p => p.game.winner === opposite(p.hostColor)).length
        )
      )
    ),
  ]);
}

const NumberFirstRegex = /^(\d+)\s(.+)$/,
  NumberLastRegex = /^(.+)\s(\d+)$/;

const splitNumber = (s: string) => {
  let found: string[] | null;
  if ((found = s.match(NumberFirstRegex)))
    return [h('div.number', found[1]), h('div.text', found[2])];
  if ((found = s.match(NumberLastRegex)))
    return [h('div.number', found[2]), h('div.text', found[1])];
  return h('div.text', s);
};

const opposite = (c: Color) => (c === 'sente' ? 'gote' : 'sente');
