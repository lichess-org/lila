import type StormCtrl from '../ctrl';
import { getNow } from 'lib/puz/util';
import renderHistory from 'lib/puz/view/history';
import { numberSpread } from 'lib/i18n';
import { onInsert, type LooseVNodes, hl } from 'lib/snabbdom';

const renderEnd = (ctrl: StormCtrl): LooseVNodes => [renderSummary(ctrl), renderHistory(ctrl)];

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
      hl(
        'div.storm--end__high.storm--end__high-daily.bar-glider',
        hl('div.storm--end__high__content', [
          hl('div.storm--end__high__text', [
            hl('strong', newHighI18n[high.key]),
            high.prev ? hl('span', i18n.storm.previousHighscoreWasX(high.prev)) : null,
          ]),
        ]),
      ),
    hl('div.storm--end__score', [
      hl(
        'span.storm--end__score__number',
        { hook: onInsert(el => numberSpread(el, scoreSteps, Math.round(scoreSteps * 50), 0)(run.score)) },
        '0',
      ),
      hl('p', i18n.storm.puzzlesSolved),
    ]),
    hl('div.storm--end__stats.box.box-pad', [
      hl('table.slist', [
        hl('tbody', [
          hl('tr', [hl('th', i18n.storm.moves), hl('td', hl('number', `${run.moves}`))]),
          hl('tr', [
            hl('th', i18n.storm.accuracy),
            hl('td', [hl('number', Number(accuracy).toFixed(1)), '%']),
          ]),
          hl('tr', [hl('th', i18n.storm.combo), hl('td', hl('number', `${ctrl.run.combo.best}`))]),
          hl('tr', [hl('th', i18n.storm.time), hl('td', [hl('number', `${Math.round(run.time)}`), 's'])]),
          hl('tr', [
            hl('th', i18n.storm.timePerMove),
            hl('td', [hl('number', Number(run.time / run.moves).toFixed(2)), 's']),
          ]),
          hl('tr', [hl('th', i18n.storm.highestSolved), hl('td', hl('number', `${run.highest}`))]),
        ]),
      ]),
    ]),
    hl(
      'a.storm-play-again.button',
      { attrs: ctrl.run.endAt! < getNow() - 900 ? { href: '/storm' } : {} },
      i18n.storm.playAgain,
    ),
  ];
};

export default renderEnd;
