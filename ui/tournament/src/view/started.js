var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var arena = require('./arena');

// <div class="tournament_clock title_tag" data-time="@tour.remainingSeconds">
//   <div class="time" data-icon="p">@tour.clockStatus</div>
// </div>

// <h1 data-icon="g">
//   @tour.fullName
//   @if(tour.isSwiss) { [beta] }
// </h1>

// @pov.map { p =>
// <a class="is pov button glowing" href="@routes.Round.player(p.fullId)">
//   You are playing @usernameOrAnon(p.opponent.userId)
//   <span class="pov_join" data-icon="G">&nbsp;@trans.joinTheGame()</span>
// </a>
// }

// @tour.system match {
//   case lila.tournament.System.Arena => {
//     @tournament.arenaStanding(tour)
//   }
//   case lila.tournament.System.Swiss => {
//     @tournament.swissStanding(tour)
//   }
// }

// @tournament.games(games)

module.exports = {
  main: function(ctrl) {
    var gameId = tournament.myCurrentGame(ctrl);
    return [
      m('div.tournament_clock.title_tag', {
        config: function(el, isUpdate) {
          if (!isUpdate) $(el).clock({
            time: ctrl.data.seconds
          });
        }
      }, m('div.time.text[data-icon=p]')),
      util.title(ctrl),
      gameId ? m('a.is.pov.button.glowing', {
        href: '/' + gameId
      }, [
        'You are playing!',
        m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
      ]) : null,
      m('div.standing_wrap.scroll-shadow-soft',
        m('table.slist.standing' + ctrl.data.scheduled ? '.scheduled' : '',
          ctrl.data.system === 'arena' ? arena.standing(ctrl) : null)),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: function(ctrl) {
    return null;
  }
};
