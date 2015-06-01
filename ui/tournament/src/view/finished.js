var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var arena = require('./arena');
var swiss = require('./swiss');
var pairings = require('./pairings');
var pagination = require('./pagination');

module.exports = {
  main: function(ctrl) {
    var pag = pagination.players(ctrl);
    return [
      util.title(ctrl),
      arena.podium(ctrl),
      m('div.standing_wrap',
        pagination.render(ctrl, pag,
          m('table.slist.standing' + (ctrl.data.scheduled ? '.scheduled' : ''), (ctrl.data.system === 'arena' ? arena.standing : swiss.standing)(ctrl, pag)))),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: pairings
};
