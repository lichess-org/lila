import type Highcharts from 'highcharts';

import AnalyseCtrl from './ctrl';
import { baseUrl } from './util';
import modal from 'common/modal';
import { url as xhrUrl, textRaw as xhrTextRaw } from 'common/xhr';
import { AnalyseData } from './interfaces';

interface HighchartsHTMLElement extends HTMLElement {
  highcharts: Highcharts.ChartObject;
}

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
    gameGifLink = document.querySelector('.game-gif') as HTMLAnchorElement,
    positionGifLink = document.querySelector('.position-gif') as HTMLAnchorElement,
    unselect = (chart: Highcharts.ChartObject) => chart.getSelectedPoints().forEach(point => point.select(false));
  let lastInputHash: string;

  if (!window.LichessAnalyseNvui) {
    lichess.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      setTimeout(
        () => (v ? $menu.find('[data-panel="computer-analysis"]') : $menu.find('span:eq(1)')).trigger('mousedown'),
        50
      );
      if (v) $('#acpl-chart').each((_, e) => (e as HighchartsHTMLElement).highcharts.reflow());
    });
    lichess.pubsub.on('analysis.change', (fen: Fen, _, mainlinePly: Ply | false) => {
      const $chart = $('#acpl-chart');
      const nextInputHash = `${fen}${ctrl.bottomColor()}`;
      if (fen && nextInputHash !== lastInputHash) {
        inputFen.value = fen;
        positionGifLink.href = xhrUrl(`/export/gif/${fen.replace(/ /g, '_')}`, {
          color: ctrl.bottomColor(),
          lastMove: ctrl.node.uci,
          variant: ctrl.data.game.variant.key,
        });
        gameGifLink.pathname = `/game/export/gif/${ctrl.bottomColor()}/${data.game.id}.gif`;
        lastInputHash = nextInputHash;
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
      if (!window.LichessChartGame) startAdvantageChart();
      else if (window.LichessChartGame.acpl.update) window.LichessChartGame.acpl.update(d, ctrl.mainline);
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-loader').remove();
    });
  }

  const chartLoader = () =>
    `<div id="acpl-chart-loader"><span>Stockfish 15<br>server analysis</span>${lichess.spinnerHtml}</div>`;

  function startAdvantageChart() {
    if (window.LichessChartGame?.acpl.update || window.LichessAnalyseNvui) return;
    const loading = !ctrl.tree.root.eval || !Object.keys(ctrl.tree.root.eval).length;
    const $panel = $panels.filter('.computer-analysis');
    if (!$('#acpl-chart').length) $panel.html('<div id="acpl-chart"></div>' + (loading ? chartLoader() : ''));
    else if (loading && !$('#acpl-chart-loader').length) $panel.append(chartLoader());
    lichess
      .loadModule('chart.game')
      .then(() => window.LichessChartGame!.acpl(data, ctrl.mainline, ctrl.trans, $('#acpl-chart')[0] as HTMLElement));
  }

  const storage = lichess.storage.make('analysis.panel');
  const setPanel = function (panel: string) {
    $menu.children('.active').removeClass('active');
    $menu.find(`[data-panel="${panel}"]`).addClass('active');
    $panels
      .removeClass('active')
      .filter('.' + panel)
      .addClass('active');
    if ((panel == 'move-times' || ctrl.opts.hunter) && !window.LichessChartGame?.movetime.render)
      lichess
        .loadModule('chart.game')
        .then(() => window.LichessChartGame!.movetime(data, ctrl.trans, ctrl.opts.hunter));
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
      xhrTextRaw(this.action, { method: this.method }).then(res => {
        if (res.ok) startAdvantageChart();
        else
          res.text().then(t => {
            if (t && !t.startsWith('<!DOCTYPE html>')) alert(t);
            lichess.reload();
          });
      });
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
    // location.hash is percent encoded, so no need to escape and make &bg=...
    // uglier in the process.
    const url = `${baseUrl()}/embed/game/${data.game.id}?theme=auto&bg=auto${location.hash}`;
    const iframe = `<iframe src="${url}"\nwidth=600 height=397 frameborder=0></iframe>`;
    modal({
      content: $(
        '<div><strong style="font-size:1.5em">' +
          $(this).html() +
          '</strong><br /><br />' +
          '<pre>' +
          lichess.escapeHtml(iframe) +
          '</pre><br />' +
          iframe +
          '<br /><br />' +
          '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a></div>'
      ),
    });
  });
}
