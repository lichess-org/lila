package lila.api

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ GameRepo, Pov }
import lila.lobby.actorApi.HooksFor
import lila.lobby.{ Hook, HookRepo, Seek, SeekApi }
import lila.setup.FilterConfig
import lila.user.{ User, UserContext }

final class LobbyApi(
    lobby: ActorRef,
    lobbyVersion: () => Int,
    getFilter: UserContext => Fu[FilterConfig],
    lightUser: String => Option[LightUser],
    seekApi: SeekApi) {

  import makeTimeout.large

  def apply(implicit ctx: Context): Fu[JsObject] =
    (lobby ? HooksFor(ctx.me)).mapTo[Vector[Hook]] zip
      ctx.me.fold(seekApi.forAnon)(seekApi.forUser) zip
      (ctx.me ?? GameRepo.urgentGames) zip
      getFilter(ctx) map {
        case (((hooks, seeks), povs), filter) => Json.obj(
          "me" -> ctx.me.map { u =>
            Json.obj("username" -> u.username)
          },
          "version" -> lobbyVersion(),
          "hooks" -> JsArray(hooks map (_.render)),
          "seeks" -> JsArray(seeks map (_.render)),
          "nowPlaying" -> JsArray(povs take 9 map nowPlaying),
          "nbNowPlaying" -> povs.size,
          "filter" -> filter.render)
      }

  def nowPlaying(pov: Pov) = Json.obj(
    "fullId" -> pov.fullId,
    "gameId" -> pov.gameId,
    "fen" -> (chess.format.Forsyth exportBoard pov.game.toChess.board),
    "color" -> pov.color.name,
    "lastMove" -> ~pov.game.castleLastMoveTime.lastMoveString,
    "variant" -> Json.obj(
      "key" -> pov.game.variant.key,
      "name" -> pov.game.variant.name),
    "speed" -> pov.game.speed.key,
    "perf" -> lila.game.PerfPicker.key(pov.game),
    "rated" -> pov.game.rated,
    "opponent" -> Json.obj(
      "id" -> pov.opponent.userId,
      "username" -> lila.game.Namer.playerString(pov.opponent, withRating = false)(lightUser),
      "rating" -> pov.opponent.rating,
      "ai" -> pov.opponent.aiLevel).noNull,
    "isMyTurn" -> pov.isMyTurn,
    "secondsLeft" -> pov.remainingSeconds)
}
