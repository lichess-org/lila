package lila.api

import play.api.libs.json.{ JsArray, JsObject, Json }

import lila.common.Json._
import lila.game.Pov
import lila.lobby.SeekApi

final class LobbyApi(
    lightUserApi: lila.user.LightUserApi,
    seekApi: SeekApi,
    gameProxyRepo: lila.round.GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(implicit ctx: Context): Fu[(JsObject, List[Pov])] =
    ctx.me.fold(seekApi.forAnon)(seekApi.forUser).mon(_.lobby segment "seeks") zip
      (ctx.me ?? gameProxyRepo.urgentGames).mon(_.lobby segment "urgentGames") flatMap { case (seeks, povs) =>
        val displayedPovs = povs take 9
        lightUserApi.preloadMany(displayedPovs.flatMap(_.opponent.userId)) inject {
          Json.obj(
            "me" -> ctx.me.map { u =>
              Json
                .obj("username" -> u.username)
                .add("isBot" -> u.isBot)
                .add("rating" -> u.perfs.standard.some.withFilter(r => !r.clueless).map(_.intRating))
                .add("aiLevel" -> u.perfs.aiLevels.standard)
                .add("isNewPlayer" -> !u.hasGames)
            },
            "seeks"        -> JsArray(seeks.map(_.render)),
            "nowPlaying"   -> JsArray(displayedPovs map nowPlaying),
            "nbNowPlaying" -> povs.size
          ) -> displayedPovs
        }
      }

  def nowPlaying(pov: Pov) =
    Json
      .obj(
        "fullId"   -> pov.fullId,
        "gameId"   -> pov.gameId,
        "sfen"     -> pov.game.situation.toSfen,
        "color"    -> pov.color.name,
        "lastMove" -> ~pov.game.lastUsiStr,
        "variant" -> Json.obj(
          "key"  -> pov.game.variant.key,
          "name" -> pov.game.variant.name
        ),
        "speed"    -> pov.game.speed.key,
        "perf"     -> lila.game.PerfPicker.key(pov.game),
        "rated"    -> pov.game.rated,
        "hasMoved" -> pov.hasMoved,
        "opponent" -> Json
          .obj(
            "id" -> pov.opponent.userId,
            "username" -> lila.game.Namer
              .playerTextBlocking(pov.opponent, withRating = false)(lightUserApi.sync)
          )
          .add("rating" -> pov.opponent.rating)
          .add("ai" -> pov.opponent.aiLevel)
          .add("aiCode" -> pov.opponent.aiCode),
        "isMyTurn" -> pov.isMyTurn
      )
      .add("secondsLeft" -> pov.remainingSeconds)
}
