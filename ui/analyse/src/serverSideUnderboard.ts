import type { AcplChart } from 'chart/dist/interface';

import AnalyseCtrl from './ctrl';
import { baseUrl } from './view/util';
import modal from 'common/modal';
import { url as xhrUrl, textRaw as xhrTextRaw } from 'common/xhr';
import { AnalyseData } from './interfaces';

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  $(element).replaceWith(ctrl.opts.$underboard!);

  $('#adv-chart').attr('id', 'acpl-chart');

  const data = ctrl.data,
    $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    inputFen = document.querySelector('.analyse__underboard__fen') as HTMLInputElement,
    gameGifLink = document.querySelector('.game-gif') as HTMLAnchorElement,
    positionGifLink = document.querySelector('.position-gif') as HTMLAnchorElement;
  let lastInputHash: string;
  let advChart: AcplChart;
  let timeChartLoaded = false;

  const updateGifLinks = (fen: Fen) => {
    const ds = document.body.dataset;
    positionGifLink.href = xhrUrl(ds.assetUrl + '/export/fen.gif', {
      fen,
      color: ctrl.bottomColor(),
      lastMove: ctrl.node.uci,
      variant: ctrl.data.game.variant.key,
      theme: ds.boardTheme,
      piece: ds.pieceSet,
    });
    gameGifLink.href = xhrUrl(ds.assetUrl + `/game/export/gif/${ctrl.bottomColor()}/${data.game.id}.gif`, {
      theme: ds.boardTheme,
      piece: ds.pieceSet,
    });
  };

  if (!window.LichessAnalyseNvui) {
    lichess.pubsub.on('theme.change', () => updateGifLinks(inputFen.value));
    lichess.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      setTimeout(
        () => (v ? $menu.find('[data-panel="computer-analysis"]') : $menu.find('span:eq(1)')).trigger('mousedown'),
        50
      );
      if (v) advChart?.reflow();
    });
    lichess.pubsub.on('analysis.change', (fen: Fen, _) => {
      const nextInputHash = `${fen}${ctrl.bottomColor()}`;
      if (fen && nextInputHash !== lastInputHash) {
        inputFen.value = fen;
        updateGifLinks(fen);
        lastInputHash = nextInputHash;
      }
    });
    lichess.pubsub.on('analysis.server.progress', (d: AnalyseData) => {
      if (!window.LichessChartGame) startAdvantageChart();
      else advChart?.updateData(d, ctrl.mainline);
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-loader').remove();
    });
  }

  const chartLoader = () =>
    `<div id="acpl-chart-loader"><span>Stockfish 15<br>server analysis</span>${lichess.spinnerHtml}</div>`;

  function startAdvantageChart() {
    if (advChart || window.LichessAnalyseNvui) return;
    const loading = !ctrl.tree.root.eval || !Object.keys(ctrl.tree.root.eval).length;
    const $panel = $panels.filter('.computer-analysis');
    if (!$('#acpl-chart').length) $panel.html('<div id="acpl-chart"></div>' + (loading ? chartLoader() : ''));
    else if (loading && !$('#acpl-chart-loader').length) $panel.append(chartLoader());
    lichess
      .loadModule('chart.game')
      .then(() => window.LichessChartGame.acpl($('#acpl-chart')[0] as HTMLElement, data, ctrl.mainline, ctrl.trans))
      .then(chart => {
        advChart = chart;
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
    if ((panel == 'move-times' || ctrl.opts.hunter) && !timeChartLoaded)
      lichess.loadModule('chart.game').then(() => {
        timeChartLoaded = true;
        window.LichessChartGame.movetime($('#movetimes-chart')[0] as HTMLElement, data, ctrl.trans, ctrl.opts.hunter);
      });
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
