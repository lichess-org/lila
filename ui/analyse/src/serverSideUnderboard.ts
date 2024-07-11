import AnalyseCtrl from './ctrl';
import { baseUrl } from './view/util';
import * as licon from 'common/licon';
import { url as xhrUrl, textRaw as xhrTextRaw } from 'common/xhr';
import { AnalyseData } from './interfaces';
import { ChartGame, AcplChart } from 'chart';
import { stockfishName } from 'common/spinner';
import { domDialog } from 'common/dialog';
import { FEN } from 'chessground/types';
import { escapeHtml } from 'common';

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  $(element).replaceWith(ctrl.opts.$underboard);

  const data = ctrl.data,
    $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    inputFen = document.querySelector('.analyse__underboard__fen input') as HTMLInputElement,
    gameGifLink = document.querySelector('.game-gif a') as HTMLAnchorElement,
    positionGifLink = document.querySelector('.position-gif a') as HTMLAnchorElement;
  let lastInputHash: string;
  let advChart: AcplChart;
  let timeChartLoaded = false;

  const updateGifLinks = (fen: FEN) => {
    const ds = document.body.dataset;
    positionGifLink.href = xhrUrl(ds.assetUrl + '/export/fen.gif', {
      fen,
      color: ctrl.bottomColor(),
      lastMove: ctrl.node.uci,
      variant: ctrl.data.game.variant.key,
      theme: ds.board,
      piece: ds.pieceSet,
    });
    gameGifLink.href = xhrUrl(ds.assetUrl + `/game/export/gif/${ctrl.bottomColor()}/${data.game.id}.gif`, {
      theme: ds.board,
      piece: ds.pieceSet,
    });
  };

  if (!site.blindMode) {
    site.pubsub.on('board.change', () => updateGifLinks(inputFen.value));
    site.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      if (v) {
        setTimeout(() => $menu.find('.computer-analysis').first().trigger('mousedown'), 50);
      } else {
        $menu.find('span:not(.computer-analysis)').first().trigger('mousedown');
      }
    });
    site.pubsub.on('analysis.change', (fen: FEN, _) => {
      const nextInputHash = `${fen}${ctrl.bottomColor()}`;
      if (fen && nextInputHash !== lastInputHash) {
        inputFen.value = fen;
        updateGifLinks(fen);
        lastInputHash = nextInputHash;
      }
    });
    site.pubsub.on('analysis.server.progress', (d: AnalyseData) => {
      if (!advChart) startAdvantageChart();
      else advChart.updateData(d, ctrl.mainline);
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-container-loader').remove();
    });
  }

  const chartLoader = () =>
    `<div id="acpl-chart-container-loader"><span>${stockfishName}<br>server analysis</span>${site.spinnerHtml}</div>`;

  function startAdvantageChart() {
    if (advChart || site.blindMode) return;
    const loading = !ctrl.tree.root.eval || !Object.keys(ctrl.tree.root.eval).length;
    const $panel = $panels.filter('.computer-analysis');
    if (!$('#acpl-chart-container').length)
      $panel.html(
        '<div id="acpl-chart-container"><canvas id="acpl-chart"></canvas></div>' +
          (loading ? chartLoader() : ''),
      );
    else if (loading && !$('#acpl-chart-container-loader').length) $panel.append(chartLoader());
    site.asset.loadEsm<ChartGame>('chart.game').then(m => {
      m.acpl($('#acpl-chart')[0] as HTMLCanvasElement, data, ctrl.serverMainline(), ctrl.trans).then(
        chart => {
          advChart = chart;
        },
      );
    });
  }

  const storage = site.storage.make('analysis.panel');
  const setPanel = function (panel: string) {
    $menu.children('.active').removeClass('active');
    $menu.find(`[data-panel="${panel}"]`).addClass('active');
    $panels
      .removeClass('active')
      .filter('.' + panel)
      .addClass('active');
    if ((panel == 'move-times' || ctrl.opts.hunter) && !timeChartLoaded)
      site.asset.loadEsm<ChartGame>('chart.game').then(m => {
        timeChartLoaded = true;
        m.movetime($('#movetimes-chart')[0] as HTMLCanvasElement, data, ctrl.trans, ctrl.opts.hunter);
      });
    if ((panel == 'computer-analysis' || ctrl.opts.hunter) && $('#acpl-chart-container').length)
      setTimeout(startAdvantageChart, 200);
  };
  $menu.on('mousedown', 'span', function (this: HTMLElement) {
    const panel = this.dataset.panel!;
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
  if (foundStored) setPanel(stored);
  else {
    const $menuCt = $menu.children('[data-panel="ctable"]');
    ($menuCt.length ? $menuCt : $menu.children(':first-child')).trigger('mousedown');
  }
  if (!data.analysis) {
    $panels.find('form.future-game-analysis').on('submit', function (this: HTMLFormElement) {
      if ($(this).hasClass('must-login')) {
        if (confirm(ctrl.trans('youNeedAnAccountToDoThat')))
          location.href = '/login?referrer=' + window.location.pathname;
        return false;
      }
      xhrTextRaw(this.action, { method: this.method }).then(res => {
        if (res.ok) startAdvantageChart();
        else
          res.text().then(t => {
            if (t && !t.startsWith('<!DOCTYPE html>')) alert(t);
            site.reload();
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
    domDialog({
      show: 'modal',
      htmlText:
        '<div><strong style="font-size:1.5em">' +
        $(this).html() +
        '</strong><br /><br />' +
        '<pre>' +
        escapeHtml(iframe) +
        '</pre><br />' +
        iframe +
        '<br /><br />' +
        `<a class="text" data-icon="${licon.InfoCircle}" href="/developers#embed-game">Read more about embedding games</a></div>`,
    });
  });
}
