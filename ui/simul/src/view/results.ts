import * as status from 'game/status';
import { i18nPluralSame } from 'i18n';
import { h } from 'snabbdom';
import type SimulCtrl from '../ctrl';

export default function (ctrl: SimulCtrl) {
  return h('div.results', [
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbPlaying',
          ctrl.data.pairings.filter(p => p.game.status < status.ids.aborted).length,
        ),
      ),
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbWins',
          ctrl.data.pairings.filter(p => p.game.winner === p.hostColor).length,
        ),
      ),
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbDraws',
          ctrl.data.pairings.filter(p => p.game.status >= status.ids.mate && !p.game.winner).length,
        ),
      ),
    ),
    h(
      'div',
      splitNumber(
        i18nPluralSame(
          'nbLosses',
          ctrl.data.pairings.filter(p => p.game.winner === opposite(p.hostColor)).length,
        ),
      ),
    ),
  ]);
}

const NumberFirstRegex = /^(\d+)\s(.+)$/,
  NumberLastRegex = /^(.+)\s(\d+)$/;

const splitNumber = (s: string) => {
  const foundFirst = s.match(NumberFirstRegex);
  if (foundFirst) return [h('div.number', foundFirst[1]), h('div.text', foundFirst[2])];
  const foundLast = s.match(NumberLastRegex);
  if (foundLast) return [h('div.number', foundLast[2]), h('div.text', foundLast[1])];
  return h('div.text', s);
};

const opposite = (c: Color) => (c === 'sente' ? 'gote' : 'sente');
