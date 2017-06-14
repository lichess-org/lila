var defined = require('common').defined;

module.exports = function(element, cfg) {
  var data = cfg.data;
  lichess.openInMobileApp('/analyse/' + data.game.id);
  var $watchers = $('#site_header div.watchers').watchers();
  var analyse, $panels;
  lichess.socket = lichess.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: {
        name: "analyse"
      },
      params: {
        userTv: data.userTv && data.userTv.id
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      },
      events: {
        analysisProgress: function(d) {
          var partial = !d.tree.eval;
          if (!lichess.advantageChart) startAdvantageChart();
          else if (lichess.advantageChart.update) lichess.advantageChart.update(data, partial);
          if (!partial) $("#adv_chart_loader").remove();
        },
        crowd: function(event) {
          $watchers.watchers("set", event.watchers);
        }
      }
    });

  var $timeChart = $("#movetimes_chart");
  var inputFen = element.querySelector('input.fen');
  var unselect = function(chart) {
    chart.getSelectedPoints().forEach(function(point) {
      point.select(false);
    });
  };
  var lastFen, lastPly;
  cfg.onChange = function(fen, path, mainlinePly) {
    if (lastPly === mainlinePly) return;
    lastPly = typeof mainlinePly === 'undefined' ? lastPly : mainlinePly;
    var chart, point, $chart = $("#adv_chart");
    if (fen && fen !== lastFen) {
      inputFen.value = fen;
      lastFen = fen;
    }
    if ($chart.length) try {
      chart = $chart.highcharts();
      if (chart) {
        if (lastPly === false) unselect(chart);
        else {
          point = chart.series[0].data[lastPly - 1 - cfg.data.game.startedAtTurn];
          if (defined(point)) point.select();
          else unselect(chart);
        }
      }
    } catch (e) {}
    if ($timeChart.length) try {
      chart = $timeChart.highcharts();
      if (chart) {
        if (lastPly === false) unselect(chart);
        else {
          var white = lastPly % 2 !== 0;
          var serie = white ? 0 : 1;
          var turn = Math.floor((lastPly - 1 - cfg.data.game.startedAtTurn) / 2);
          point = chart.series[serie].data[turn];
          if (defined(point)) point.select();
          else unselect(chart);
        }
      }
    } catch (e) {}
  };
  cfg.onToggleComputer = function(v) {
    setTimeout(function() {
      if (v) $('div.analysis_menu a.computer_analysis').mousedown();
      else $('div.analysis_menu a:eq(1)').mousedown();
    }.bind(this), 50);
  };
  cfg.initialPly = 'url';
  cfg.element = element.querySelector('.analyse');
  cfg.socketSend = lichess.socket.send;
  analyse = LichessAnalyse.mithril(cfg);
  cfg.jumpToIndex = analyse.jumpToIndex;

  if (cfg.chat) {
    cfg.chat.parseMoves = true;
    lichess.makeChat('chat', cfg.chat);
  }

  $('.underboard_content', element).appendTo($('.underboard .center', element)).removeClass('none');

  var chartLoader = function() {
    return '<div id="adv_chart_loader">' +
      '<span>' + lichess.engineName + '<br>server analysis</span>' +
      lichess.spinnerHtml +
      '</div>'
  };
  var startAdvantageChart = function() {
    if (lichess.advantageChart) return;
    var loading = !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
    var $panel = $panels.filter('.computer_analysis');
    if (!$("#adv_chart").length) $panel.html('<div id="adv_chart"></div>' + (loading ? chartLoader() : ''));
    else if (loading && !$("#adv_chart_loader").length) $panel.append(chartLoader());
    lichess.loadScript('/assets/javascripts/chart/advantage.js').then(function() {
      lichess.advantageChart(data);
    });
  };

  $panels = $('div.analysis_panels > div');
  var $menu = $('div.analysis_menu');
  var storage = lichess.storage.make('analysis.panel');
  var setPanel = function(panel) {
    $menu.children('.active').removeClass('active').end().find('.' + panel).addClass('active');
    $panels.removeClass('active').filter('.' + panel).addClass('active');
    if (panel === 'move_times' && !lichess.movetimeChart) try {
      lichess.loadScript('/assets/javascripts/chart/movetime.js').then(function() {
        lichess.movetimeChart(data);
      });
    } catch (e) {}
    if (panel === 'computer_analysis' && $("#adv_chart").length)
      setTimeout(startAdvantageChart, 200);
  };
  $menu.on('mousedown', 'a', function() {
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
  if (!cfg.data.analysis) {
    $panels.find('form.future_game_analysis').submit(function() {
      if ($(this).hasClass('must_login')) {
        if (confirm(lichess.globalTrans('You need an account to do that'))) location.href = '/signup';
        return false;
      }
      $.ajax({
        method: 'post',
        url: $(this).attr('action'),
        success: startAdvantageChart,
        error: lichess.reload
      });
      return false;
    });
  }

  $panels.on('click', 'div.pgn', function() {
    var range, selection;
    if (document.body.createTextRange) {
      range = document.body.createTextRange();
      range.moveToElementText($(this)[0]);
      range.select();
    } else if (window.getSelection) {
      selection = window.getSelection();
      range = document.createRange();
      range.selectNodeContents($(this)[0]);
      selection.removeAllRanges();
      selection.addRange(range);
    }
  });
  $panels.on('click', '.embed_howto', function() {
    var url = 'https://lichess.org/embed/' + data.game.id + location.hash;
    var iframe = '<iframe src="' + url + '?theme=auto&bg=auto"\nwidth=600 height=397 frameborder=0></iframe>';
    $.modal($(
      '<strong style="font-size:1.5em">' + $(this).html() + '</strong><br /><br />' +
      '<pre>' + lichess.escapeHtml(iframe) + '</pre><br />' +
      iframe + '<br /><br />' +
      '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a>'
    ));
  });
  lichess.topMenuIntent();
};
