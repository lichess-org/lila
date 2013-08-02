package controllers

import play.api.mvc._
import play.api.templates.Html
import views._

import lila.app._
import lila.game.{ GameRepo, Pov }
import lila.round.WatcherRoomRepo
import lila.tournament.TournamentRepo

object Tv extends LilaController {

  def index = Open { implicit ctx ⇒
    OptionFuResult(Env.game.featured.one) { game ⇒
      Env.round.version(game.id) zip
        (WatcherRoomRepo room "tv" map { room ⇒
          html.round.watcherRoomInner(room.decodedMessages)
        }) zip
        (GameRepo onTv 10) zip
        (game.tournamentId ?? TournamentRepo.byId) map {
          case (((v, roomHtml), games), tour) ⇒
            Ok(html.tv.index(
              getInt("flip").exists(1==).fold(Pov invited game, Pov creator game),
              v,
              roomHtml,
              games,
              tour))
        }
    }
  }
}
