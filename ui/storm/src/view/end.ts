import type StormCtrl from '../ctrl';
import { getNow } from 'puz/util';
import renderHistory from 'puz/view/history';
import { numberSpread } from 'common/number';
import { onInsert, type LooseVNodes, looseH as h } from 'common/snabbdom';

const renderEnd = (ctrl: StormCtrl): LooseVNodes => [...renderSummary(ctrl), renderHistory(ctrl)];

const newHighI18n = {
  day: i18n.storm.newDailyHighscore,
  week: i18n.storm.newWeeklyHighscore,
  month: i18n.storm.newMonthlyHighscore,
  allTime: i18n.storm.newAllTimeHighscore,
};

const renderSummary = (ctrl: StormCtrl): LooseVNodes => {
  const run = ctrl.runStats();
  const high = ctrl.vm.response?.newHigh;
  const accuracy = (100 * (run.moves - run.errors)) / run.moves;
  const scoreSteps = Math.min(run.score, 50);
  return [
    high &&
      h(
        'div.storm--end__high.storm--end__high-daily.bar-glider',
        h('div.storm--end__high__content', [
          h('div.storm--end__high__text', [
            h('strong', newHighI18n[high.key]),
            high.prev ? h('span', i18n.storm.previousHighscoreWasX(high.prev)) : null,
          ]),
        ]),
      ),
    h('div.storm--end__score', [
      h(
        'span.storm--end__score__number',
        { hook: onInsert(el => numberSpread(el, scoreSteps, Math.round(scoreSteps * 50), 0)(run.score)) },
        '0',
      ),
      h('p', i18n.storm.puzzlesSolved),
    ]),
    h('div.storm--end__stats.box.box-pad', [
      h('table.slist', [
        h('tbody', [
          h('tr', [h('th', i18n.storm.moves), h('td', h('number', `${run.moves}`))]),
          h('tr', [h('th', i18n.storm.accuracy), h('td', [h('number', Number(accuracy).toFixed(1)), '%'])]),
          h('tr', [h('th', i18n.storm.combo), h('td', h('number', `${ctrl.run.combo.best}`))]),
          h('tr', [h('th', i18n.storm.time), h('td', [h('number', `${Math.round(run.time)}`), 's'])]),
          h('tr', [
            h('th', i18n.storm.timePerMove),
            h('td', [h('number', Number(run.time / run.moves).toFixed(2)), 's']),
          ]),
          h('tr', [h('th', i18n.storm.highestSolved), h('td', h('number', `${run.highest}`))]),
        ]),
      ]),
    ]),
    h(
      'a.storm-play-again.button',
      { attrs: ctrl.run.endAt! < getNow() - 900 ? { href: '/storm' } : {} },
      i18n.storm.playAgain,
    ),
  ];
};

export default renderEnd;
