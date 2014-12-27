var m = require('mithril');

var i18n = require('../i18n');

module.exports = function(env) {
  return {
    controller: function() {
      this.data = env.data;
      this.trans = i18n(env.i18n);
      this.router = env.routes;
    },
    view: function(ctrl) {
      var myTurnPovsNb = ctrl.data.nowPlaying.povs.count(function(p) {
        return p.isMyTurn;
      });
      return [
        m('div.tabs', [
          m('a', ctrl.trans('realTime')),
          m('a', ctrl.trans('correspondence')),
          m('a', [
            ctrl.trans('nbGamesInPlay', ctrl.data.nowPlaying.povs.length),
            myTurnPovsNb.length > 0 ? m('span.unread', myTurnPovsNb.length) : null
          ])
        ])
      ];
    }
  };
};
