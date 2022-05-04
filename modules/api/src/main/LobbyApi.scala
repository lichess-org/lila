package lila.api

import play.api.libs.json.{ JsArray, JsObject, Json }

import lila.game.Pov
import lila.lobby.{ LobbySocket, SeekApi }

final class LobbyApi(
    lightUserApi: lila.user.LightUserApi,
    seekApi: SeekApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    gameJson: lila.game.JsonView,
    lobbySocket: LobbySocket
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(implicit ctx: Context): Fu[(JsObject, List[Pov])] =
    ctx.me.fold(seekApi.forAnon)(seekApi.forUser).mon(_.lobby segment "seeks") zip
      (ctx.me ?? gameProxyRepo.urgentGames).mon(_.lobby segment "urgentGames") flatMap { case (seeks, povs) =>
        val displayedPovs = povs take 9
        lightUserApi.preloadMany(displayedPovs.flatMap(_.opponent.userId)) inject {
          Json
            .obj(
              "seeks"        -> seeks.map(_.render),
              "nowPlaying"   -> displayedPovs.map(nowPlaying),
              "nbNowPlaying" -> povs.size,
              "counters" -> Json.obj(
                "members" -> lobbySocket.counters.members,
                "rounds"  -> lobbySocket.counters.rounds
              )
            )
            .add("ratingMap", ctx.me.map(_.perfs.ratingMap))
            .add(
              "me",
              ctx.me.map { u =>
                Json.obj("username" -> u.username).add("isBot" -> u.isBot)
              }
            ) -> displayedPovs
        }
      }

  def nowPlaying(pov: Pov) = gameJson.ownerPreview(pov)(lightUserApi.sync)
}
