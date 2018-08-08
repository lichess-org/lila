package lidraughts.api

import play.api.libs.json.{ Json, JsObject, JsArray }

import lidraughts.game.{ GameRepo, Pov }
import lidraughts.lobby.SeekApi
import lidraughts.pool.JsonView.poolConfigJsonWriter
import lidraughts.setup.FilterConfig
import lidraughts.user.UserContext

final class LobbyApi(
    getFilter: UserContext => Fu[FilterConfig],
    lightUserApi: lidraughts.user.LightUserApi,
    seekApi: SeekApi,
    pools: List[lidraughts.pool.PoolConfig]
) {

  val poolsJson = Json toJson pools

  def apply(implicit ctx: Context): Fu[(JsObject, List[Pov])] =
    ctx.me.fold(seekApi.forAnon)(seekApi.forUser) zip
      (ctx.me ?? GameRepo.urgentGames) zip
      getFilter(ctx) flatMap {
        case seeks ~ povs ~ filter =>
          lightUserApi.preloadMany(povs.flatMap(_.opponent.userId)) inject {
            Json.obj(
              "me" -> ctx.me.map { u =>
                Json.obj("username" -> u.username)
              },
              "seeks" -> JsArray(seeks map (_.render)),
              "nowPlaying" -> JsArray(povs take 9 map nowPlaying),
              "nbNowPlaying" -> povs.size,
              "filter" -> filter.render
            ) -> povs
          }
      }

  def nowPlaying(pov: Pov) = Json.obj(
    "fullId" -> pov.fullId,
    "gameId" -> pov.gameId,
    "fen" -> (draughts.format.Forsyth exportBoard pov.game.board),
    "color" -> pov.color.name,
    "lastMove" -> ~pov.game.lastMoveKeys,
    "variant" -> Json.obj(
      "key" -> pov.game.variant.key,
      "name" -> pov.game.variant.name
    ),
    "speed" -> pov.game.speed.key,
    "perf" -> lidraughts.game.PerfPicker.key(pov.game),
    "rated" -> pov.game.rated,
    "opponent" -> Json.obj(
      "id" -> pov.opponent.userId,
      "username" -> lidraughts.game.Namer.playerText(pov.opponent, withRating = false)(lightUserApi.sync)
    ).add("rating" -> pov.opponent.rating)
      .add("ai" -> pov.opponent.aiLevel),
    "isMyTurn" -> pov.isMyTurn,
    "secondsLeft" -> pov.remainingSeconds
  )
}
