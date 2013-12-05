package controllers

import play.api.mvc._
import play.api.templates.Html
import views._

import lila.app._
import lila.game.{ GameRepo, Game ⇒ GameModel, Pov }
import lila.round.WatcherRoomRepo
import lila.tournament.TournamentRepo
import lila.user.{ UserRepo, Confrontation }

object Tv extends LilaController {

  def index = Open { implicit ctx ⇒
    OptionFuResult(Env.game.featured.one) { game ⇒
      Env.round.version(game.id) zip
        (WatcherRoomRepo room "tv" map { room ⇒
          html.round.watcherRoomInner(room.decodedMessages)
        }) zip
        (GameRepo onTv 10) zip
        confrontation(game) zip
        (game.tournamentId ?? TournamentRepo.byId) map {
          case ((((v, roomHtml), games), confrontation), tour) ⇒
            Ok(html.tv.index(
              getInt("flip").exists(1==).fold(Pov second game, Pov first game),
              v, roomHtml, games, confrontation, tour))
        }
    }
  }

  private def confrontation(game: GameModel): Fu[Option[Confrontation]] = ~{
    (game.firstPlayer.userId |@| game.secondPlayer.userId) apply {
      case (id1, id2) ⇒ (UserRepo byId id1) zip (UserRepo byId id2) flatMap {
        case (Some(user1), Some(user2)) ⇒ Env.game.cached.confrontation(user1, user2) map (_.some)
        case _                          ⇒ fuccess(none)
      }
    }
  }
}
