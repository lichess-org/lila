package lila.api

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.json.{ Json, JsObject, JsArray }

import lila.common.LightUser
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
    (lobby ? HooksFor(ctx.me)).mapTo[List[Hook]] zip
      ctx.me.fold(seekApi.forAnon)(seekApi.forUser) zip
      (ctx.me ?? GameRepo.nowPlaying) zip
      getFilter(ctx) map {
        case (((hooks, seeks), povs), filter) => Json.obj(
          "version" -> lobbyVersion(),
          "hooks" -> JsArray(hooks map (_.render)),
          "seeks" -> JsArray(seeks map (_.render)),
          "nowPlaying" -> JsArray(povs map nowPlaying),
          "filter" -> filter.render)
      }

  def nowPlaying(pov: Pov) = Json.obj(
    "id" -> pov.fullId,
    "fen" -> (chess.format.Forsyth exportBoard pov.game.toChess.board),
    "color" -> pov.color.name,
    "lastMove" -> ~pov.game.castleLastMoveTime.lastMoveString,
    "variant" -> pov.game.variant.key,
    "speed" -> pov.game.speed.key,
    "perf" -> lila.game.PerfPicker.key(pov.game),
    "rated" -> pov.game.rated,
    "opponent" -> Json.obj(
      "id" -> pov.opponent.userId,
      "username" -> lila.game.Namer.playerString(pov.opponent, withRating = false)(lightUser),
      "rating" -> pov.opponent.rating),
    "isMyTurn" -> pov.isMyTurn,
    "secondsLeft" -> pov.remainingSeconds)
}
