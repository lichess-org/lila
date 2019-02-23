module.exports = function(cfg) {
  var element = document.getElementById('main-wrap');
  var data = cfg.data;
  var $watchers = $('#site_header div.watchers').watchers();
  var analyse;
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
          if (!partial) {
            lichess.pubsub.emit('analysis.server.complete')();
            $("#adv-chart-loader").remove();
          }
        },
        crowd: function(event) {
          $watchers.watchers("set", event.watchers);
        }
      }
    });
  cfg.$side = $('.analyse__side').clone();
  cfg.trans = lichess.trans(cfg.i18n);
  cfg.initialPly = 'url';
  cfg.element = element.querySelector('main.analyse');
  cfg.socketSend = lichess.socket.send;
  analyse = LichessAnalyse.start(cfg);

  lichess.topMenuIntent();
};
