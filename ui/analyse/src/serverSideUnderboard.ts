import type Highcharts from 'highcharts';

import AnalyseCtrl from './ctrl';
import { baseUrl } from './util';
import modal from 'common/modal';
import { formToXhr } from 'common/xhr';
import { AnalyseData } from './interfaces';

interface PlyChart extends Highcharts.ChartObject {
  lastPly?: Ply | false;
  selectPly(ply: number): void;
}

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  $(element).replaceWith(ctrl.opts.$underboard!);

  $('#adv-chart').attr('id', 'acpl-chart');

  const data = ctrl.data,
    $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    $timeChart = $('#movetimes-chart'),
    inputFen = document.querySelector('.analyse__underboard__fen') as HTMLInputElement,
    unselect = (chart: Highcharts.ChartObject) => {
      chart.getSelectedPoints().forEach(function (point) {
        point.select(false);
      });
    };
  let lastFen: string;

  if (!lichess.AnalyseNVUI) {
    lichess.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      setTimeout(function () {
        (v ? $menu.find('[data-panel="computer-analysis"]') : $menu.find('span:eq(1)')).trigger('mousedown');
      }, 50);
      if (v) $('#acpl-chart').each((_, e) => (e as HighchartsHTMLElement).highcharts.reflow());
    });
    lichess.pubsub.on('analysis.change', (fen: Fen, _, mainlinePly: Ply | false) => {
      const $chart = $('#acpl-chart');
      if (fen && fen !== lastFen) {
        inputFen.value = fen;
        lastFen = fen;
      }
      if ($chart.length) {
        const chart = ($chart[0] as HighchartsHTMLElement).highcharts as PlyChart;
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else chart.selectPly(mainlinePly);
          }
          chart.lastPly = mainlinePly;
        }
      }
      if ($timeChart.length) {
        const chart = ($timeChart[0] as HighchartsHTMLElement).highcharts as PlyChart;
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else chart.selectPly(mainlinePly);
          }
          chart.lastPly = mainlinePly;
        }
      }
    });
    lichess.pubsub.on('analysis.server.progress', (d: AnalyseData) => {
      if (!lichess.advantageChart) startAdvantageChart();
      else if (lichess.advantageChart.update) lichess.advantageChart.update(d);
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-loader').remove();
    });
  }

  function chartLoader() {
    return `<div id="acpl-chart-loader"><span>Stockfish 14+<br>server analysis</span>${lichess.spinnerHtml}</div>`;
  }
  function startAdvantageChart() {
    if (lichess.advantageChart || lichess.AnalyseNVUI) return;
    const loading = !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
    const $panel = $panels.filter('.computer-analysis');
    if (!$('#acpl-chart').length) $panel.html('<div id="acpl-chart"></div>' + (loading ? chartLoader() : ''));
    else if (loading && !$('#acpl-chart-loader').length) $panel.append(chartLoader());
    lichess.loadScript('javascripts/chart/acpl.js').then(function () {
      lichess.advantageChart!(data, ctrl.trans, $('#acpl-chart')[0] as HTMLElement);
    });
  }

  const storage = lichess.storage.make('analysis.panel');
  const setPanel = function (panel: string) {
    $menu.children('.active').removeClass('active');
    $menu.find(`[data-panel="${panel}"]`).addClass('active');
    $panels
      .removeClass('active')
      .filter('.' + panel)
      .addClass('active');
    if ((panel == 'move-times' || ctrl.opts.hunter) && !lichess.movetimeChart)
      lichess
        .loadScript('javascripts/chart/movetime.js')
        .then(() => lichess.movetimeChart(data, ctrl.trans, ctrl.opts.hunter));
    if ((panel == 'computer-analysis' || ctrl.opts.hunter) && $('#acpl-chart').length)
      setTimeout(startAdvantageChart, 200);
  };
  $menu.on('mousedown', 'span', function (this: HTMLElement) {
    const panel = $(this).data('panel');
    storage.set(panel);
    setPanel(panel);
  });
  const stored = storage.get();
  const foundStored =
    stored &&
    $menu.children(`[data-panel="${stored}"]`).filter(function (this: HTMLElement) {
      const display = window.getComputedStyle(this).display;
      return !!display && display != 'none';
    }).length;
  if (foundStored) setPanel(stored!);
  else {
    const $menuCt = $menu.children('[data-panel="ctable"]');
    ($menuCt.length ? $menuCt : $menu.children(':first-child')).trigger('mousedown');
  }
  if (!data.analysis) {
    $panels.find('form.future-game-analysis').on('submit', function (this: HTMLFormElement) {
      if ($(this).hasClass('must-login')) {
        if (confirm(ctrl.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
        return false;
      }
      formToXhr(this).then(startAdvantageChart, lichess.reload);
      return false;
    });
  }

  $panels.on('click', '.pgn', function (this: HTMLElement) {
    const selection = window.getSelection(),
      range = document.createRange();
    range.selectNodeContents(this);
    selection!.removeAllRanges();
    selection!.addRange(range);
  });
  $panels.on('click', '.embed-howto', function (this: HTMLElement) {
    const url = `${baseUrl()}/embed/${data.game.id}${location.hash}`;
    const iframe = '<iframe src="' + url + '?theme=auto&bg=auto"\nwidth=600 height=397 frameborder=0></iframe>';
    modal({
      content: $(
        '<strong style="font-size:1.5em">' +
          $(this).html() +
          '</strong><br /><br />' +
          '<pre>' +
          lichess.escapeHtml(iframe) +
          '</pre><br />' +
          iframe +
          '<br /><br />' +
          '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a>'
      ),
    });
  });
}
