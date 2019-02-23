import AnalyseCtrl from './ctrl';
import { defined } from 'common';

export default function(element: HTMLElement, ctrl: AnalyseCtrl) {

  const li = window.lichess;

  $(element).replaceWith($('.analyse__underboard.none').removeClass('none'));

  const data = ctrl.data,
    $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    $timeChart = $("#movetimes-chart"),
    inputFen = document.querySelector('.analyse__underboard__fen') as HTMLInputElement,
    unselect = chart => {
      chart.getSelectedPoints().forEach(function(point) {
        point.select(false);
      });
    };
  let lastFen: string;

  if (!li.AnalyseNVUI) {
    li.pubsub.on('analysis.comp.toggle', (v: boolean) => {
      setTimeout(function() {
        if (v) $menu.find('a.computer-analysis').mousedown();
        else $menu.find('a:eq(1)').mousedown();
      }, 50);
    });
    li.pubsub.on('analysis.change', (fen: Fen, _, mainlinePly: Ply | false) => {
      var chart, point, $chart = $("#adv-chart");
      if (fen && fen !== lastFen) {
        inputFen.value = fen;
        lastFen = fen;
      }
      if ($chart.length) {
        chart = window.Highcharts && $chart.highcharts();
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else {
              point = chart.series[0].data[mainlinePly - 1 - data.game.startedAtTurn];
              if (defined(point)) point.select();
              else unselect(chart);
            }
          }
          chart.lastPly = mainlinePly;
        }
      }
      if ($timeChart.length) {
        console.log(window.Highcharts);
        chart = window.Highcharts && $timeChart.highcharts();
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else {
              var white = mainlinePly % 2 !== 0;
              var serie = white ? 0 : 1;
              var turn = Math.floor((mainlinePly - 1 - data.game.startedAtTurn) / 2);
              point = chart.series[serie].data[turn];
              if (defined(point)) point.select();
              else unselect(chart);
            }
          }
          chart.lastPly = mainlinePly;
        }
      }
    });
  }

  var chartLoader = function() {
    return '<div id="adv-chart-loader">' +
      '<span>' + li.engineName + '<br>server analysis</span>' +
      li.spinnerHtml +
      '</div>'
  };
  var startAdvantageChart = function() {
    if (li.advantageChart || li.AnalyseNVUI) return;
    var loading = !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
    var $panel = $panels.filter('.computer-analysis');
    if (!$("#adv-chart").length) $panel.html('<div id="adv-chart"></div>' + (loading ? chartLoader() : ''));
    else if (loading && !$("#adv-chart-loader").length) $panel.append(chartLoader());
    li.loadScript('javascripts/chart/acpl.js').then(function() {
      li.advantageChart(data, ctrl.trans, $("#adv-chart")[0] as HTMLElement);
    });
  };

  var storage = li.storage.make('analysis.panel');
  var setPanel = function(panel) {
    $menu.children('.active').removeClass('active').end().find('.' + panel).addClass('active');
    $panels.removeClass('active').filter('.' + panel).addClass('active');
    if (panel == 'move-times' && !li.movetimeChart) try {
      li.loadScript('javascripts/chart/movetime.js').then(function() {
        li.movetimeChart(data, ctrl.trans);
      });
    } catch (e) {}
    if (panel == 'computer-analysis' && $("#adv-chart").length)
      setTimeout(startAdvantageChart, 200);
  };
  $menu.on('mousedown', 'span', function(this: HTMLElement) {
    var panel = $(this).data('panel');
    storage.set(panel);
    setPanel(panel);
  });
  var stored = storage.get();
  if (stored && $menu.children('.' + stored).length) setPanel(stored);
  else {
    var $ct = $menu.children('.crosstable');
    ($ct.length ? $ct : $menu.children(':first-child')).trigger('mousedown');
  }
  if (!data.analysis) {
    $panels.find('form.future-game-analysis').submit(function(this: HTMLElement) {
      if ($(this).hasClass('must-login')) {
        if (confirm(ctrl.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
        return false;
      }
      $.ajax({
        method: 'post',
        url: $(this).attr('action'),
        success: startAdvantageChart,
        error: li.reload
      });
      return false;
    });
  }

  $panels.on('click', '.pgn', function(this: HTMLElement) {
    let selection = window.getSelection();
    let range = document.createRange();
    range.selectNodeContents(this);
    selection.removeAllRanges();
    selection.addRange(range);
  });
  $panels.on('click', '.embed-howto', function(this: HTMLElement) {
    var url = 'https://lichess.org/embed/' + data.game.id + location.hash;
    var iframe = '<iframe src="' + url + '?theme=auto&bg=auto"\nwidth=600 height=397 frameborder=0></iframe>';
    $.modal($(
      '<strong style="font-size:1.5em">' + $(this).html() + '</strong><br /><br />' +
      '<pre>' + li.escapeHtml(iframe) + '</pre><br />' +
      iframe + '<br /><br />' +
      '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a>'
    ));
  });
}
