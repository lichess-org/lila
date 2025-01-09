import { engineName } from 'shogi/engine-name';
import AnalyseCtrl from './ctrl';
import { AnalyseData } from './interfaces';
import { baseUrl } from './util';
import { loadCompiledScript } from 'common/assets';
import { escapeHtml } from 'common/string';
import { spinnerHtml } from 'common/spinner';
import { i18n } from 'i18n';

export default function (element: HTMLElement, ctrl: AnalyseCtrl): void {
  const li = window.lishogi;

  $(element).replaceWith(ctrl.opts.$underboard!);

  const data = ctrl.data,
    $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    inputSfen = document.querySelector('.analyse__underboard__sfen') as HTMLInputElement;

  let lastInputHash: string;
  let advChart: any;
  let timeChartLoaded = false;

  if (!li.modules.analyseNvui) {
    li.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      setTimeout(function () {
        (v ? $menu.find('[data-panel="computer-analysis"]') : $menu.find('span:eq(1)')).trigger(
          'mousedown'
        );
      }, 50);
    });
    li.pubsub.on('analysis.change', (sfen: Sfen, _) => {
      const nextInputHash = `${sfen}${ctrl.bottomColor()}`;
      if (sfen && nextInputHash !== lastInputHash) {
        inputSfen.value = sfen;
        lastInputHash = nextInputHash;
      }
    });
    li.pubsub.on('analysis.server.progress', (d: AnalyseData) => {
      if (!advChart) startAdvantageChart();
      else advChart.updateData(d, ctrl.mainline);
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-loader').remove();
    });
  }

  function chartLoader() {
    const name = engineName(ctrl.data.game.variant.key, ctrl.data.game.initialSfen);
    return `<div id="acpl-chart-container-loader"><span>${name}<br>${i18n('serverAnalysis')}</span>${spinnerHtml}</div>`;
  }
  function startAdvantageChart() {
    if (advChart || li.modules.analyseNvui) return;
    const loading = !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
    const $panel = $panels.filter('.computer-analysis');
    if (!$('#acpl-chart-container').length)
      $panel.html(
        '<div id="acpl-chart-container"><canvas id="acpl-chart"></canvas></div>' +
          (loading ? chartLoader() : '')
      );
    else if (loading && !$('#acpl-chart-container-loader').length) $panel.append(chartLoader());
    loadCompiledScript('chart').then(() => {
      loadCompiledScript('chart.acpl').then(() => {
        li.modules.chartAcpl!($('#acpl-chart')[0] as HTMLCanvasElement, data, ctrl.mainline);
      });
    });
  }

  const storage = li.storage.make('analysis.panel');
  const setPanel = function (panel: string) {
    $menu.children('.active').removeClass('active');
    $menu.find(`[data-panel="${panel}"]`).addClass('active');
    $panels
      .removeClass('active')
      .filter('.' + panel)
      .addClass('active');
    if (panel == 'move-times' && !timeChartLoaded)
      loadCompiledScript('chart').then(() => {
        loadCompiledScript('chart.movetime').then(() => {
          timeChartLoaded = true;
          li.modules.chartMovetime!(
            $('#movetimes-chart')[0] as HTMLCanvasElement,
            data,
            ctrl.opts.hunter
          );
        });
      });
    if (panel == 'computer-analysis' && $('#acpl-chart-container').length)
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
  if (foundStored) setPanel(stored);
  else {
    const $menuCt = $menu.children('[data-panel="sfen-notation"]');
    ($menuCt.length ? $menuCt : $menu.children(':first-child')).trigger('mousedown');
  }
  if (!data.analysis) {
    $panels.find('form.future-game-analysis').submit(function (this: HTMLFormElement) {
      if ($(this).hasClass('must-login')) {
        if (confirm(i18n('youNeedAnAccountToDoThat'))) location.href = '/signup';
        return false;
      }
      window.lishogi.xhr.formToXhr(this).then(startAdvantageChart).catch(li.reload);
      return false;
    });
  }

  $panels.on('click', '.kif', function (this: HTMLElement) {
    const selection = window.getSelection(),
      range = document.createRange();
    range.selectNodeContents(this);
    selection!.removeAllRanges();
    selection!.addRange(range);
  });
  $panels.on('click', '.embed-howto', function (this: HTMLElement) {
    const url = `${baseUrl()}/embed/${data.game.id}${location.hash}`;
    const iframe =
      '<iframe src="' + url + '?theme=auto&bg=auto"\nwidth=600 height=400 frameborder=0></iframe>';
    $.modal(
      $(
        '<strong style="font-size:1.5em">' +
          $(this).html() +
          '</strong><br /><br />' +
          '<pre>' +
          escapeHtml(iframe) +
          '</pre><br />' +
          iframe +
          '<br /><br />' +
          '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a>'
      )
    );
  });
}
