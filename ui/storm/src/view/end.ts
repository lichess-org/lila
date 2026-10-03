import { numberSpread } from 'lib/i18n';
import { getNow } from 'lib/puz/util';
import {
  onInsert,
  type MaybeVNodes,
  div,
  strong,
  span,
  p,
  table,
  a,
  button,
  tbody,
  tr,
  th,
  td,
  makeExoticTag,
} from 'lib/view';

import type StormCtrl from '@/ctrl';

const newHighI18n = {
  day: i18n.storm.newDailyHighscore,
  week: i18n.storm.newWeeklyHighscore,
  month: i18n.storm.newMonthlyHighscore,
  allTime: i18n.storm.newAllTimeHighscore,
};

const number = makeExoticTag('number');

export default function renderSummary(ctrl: StormCtrl): MaybeVNodes {
  const run = ctrl.runStats();
  const high = ctrl.vm.response?.newHigh;
  const playAgain = ctrl.run.endAt! < getNow() - ctrl.duration ? a('/storm') : button;
  const accuracy = (100 * (run.moves - run.errors)) / run.moves;
  const scoreSteps = Math.min(run.score, 50);
  return [
    high
      ? div(
          '.storm--end__high.storm--end__high-daily.bar-glider',
          div('.storm--end__high__content', [
            div('.storm--end__high__text', [
              strong(newHighI18n[high.key]),
              high.prev ? span(i18n.storm.previousHighscoreWasX(high.prev)) : null,
            ]),
          ]),
        )
      : null,
    div('.storm--end__score', [
      span(
        '.storm--end__score__number',
        { hook: onInsert(el => numberSpread(el, scoreSteps, Math.round(scoreSteps * 50), 0)(run.score)) },
        '0',
      ),
      p(i18n.storm.puzzlesSolved),
    ]),
    div('.storm--end__stats.box.box-pad', [
      table('.slist', [
        tbody([
          tr([th(i18n.storm.moves), td(number(run.moves))]),
          tr([
            th(i18n.storm.accuracy),
            td([number(accuracy ? accuracy.toFixed(1) : '-'), accuracy ? '%' : '']),
          ]),
          tr([th(i18n.storm.combo), td(number(ctrl.run.combo.best))]),
          tr([th(i18n.storm.time), td([number(run.time ? Math.round(run.time) : 0), 's'])]),
          tr([
            th(i18n.storm.timePerMove),
            td([number(run.time ? (run.time / run.moves).toFixed(2) : 0), 's']),
          ]),
          tr([th(i18n.storm.highestSolved), td(number(run.highest))]),
        ]),
      ]),
    ]),
    playAgain('.storm-play-again.button', i18n.storm.playAgain),
  ];
}
