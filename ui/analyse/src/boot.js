var defined = require('common').defined;
var tree = require("tree")

module.exports = function(cfg) {
  var element = document.getElementById('main-wrap');
  var data = cfg.data;
  var maxNodes = 200; // no analysis beyond ply 200
  var $watchers = $('#site_header div.watchers').watchers();
  var analyse;
  var partialTree = function(n, c) {
    if (c === undefined) c = 0;
    if (c > maxNodes) return false;
    return n.children.length && (!n.eval || partialTree(n.children[0], c + 1));
  }
  lidraughts.socket = lidraughts.StrongSocket(
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
          var partial = partialTree(d.tree);
          if (!lidraughts.advantageChart) startAdvantageChart();
          else if (lidraughts.advantageChart.update) lidraughts.advantageChart.update({ game: data.game, treeParts: tree.ops.mainlineNodeList(tree.build(d.tree).root) }, partial);
          if (!partial) {
            lidraughts.pubsub.emit('analysis.server.complete')();
            $("#adv-chart-loader").remove();
          }
        },
        crowd: function(event) {
          $watchers.watchers("set", event.watchers);
        }
      }
    });
  cfg.$side = $('.analyse__side').clone();
  cfg.trans = lidraughts.trans(cfg.i18n);
  cfg.initialPly = 'url';
  cfg.element = element.querySelector('main.analyse');
  cfg.socketSend = lidraughts.socket.send;
  analyse = LidraughtsAnalyse.start(cfg);

  lidraughts.topMenuIntent();
  $('button.cheat_list').on('click', function() {
    $.post({
      url: $(this).data('src') + '?v=' + !$(this).hasClass('active')
    });
    $(this).toggleClass('active');
  });
};
