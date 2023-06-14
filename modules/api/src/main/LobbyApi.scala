package lila.api

import play.api.libs.json.{ JsObject, Json, Writes }

import lila.game.Pov
import lila.lobby.{ LobbySocket, SeekApi }
import lila.common.Json.given
import lila.user.User
import lila.rating.Perf

final class LobbyApi(
    lightUserApi: lila.user.LightUserApi,
    seekApi: SeekApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    gameJson: lila.game.JsonView,
    lobbySocket: LobbySocket
)(using Executor):

  def apply(using ctx: WebContext): Fu[(JsObject, List[Pov])] =
    ctx.me.fold(seekApi.forAnon)(seekApi.forUser).mon(_.lobby segment "seeks") zip
      (ctx.me so gameProxyRepo.urgentGames).mon(_.lobby segment "urgentGames") flatMap { case (seeks, povs) =>
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
            .add("ratingMap", ctx.me.map(ratingMap))
            .add(
              "me",
              ctx.me.map { u =>
                Json.obj("username" -> u.username).add("isBot" -> u.isBot)
              }
            ) -> displayedPovs
        }
      }

  def nowPlaying(pov: Pov) = gameJson.ownerPreview(pov)(using lightUserApi.sync)

  private def ratingMap(u: User): JsObject =
    Writes
      .keyMapWrites[Perf.Key, JsObject, Map]
      .writes(
        u.perfs.perfsMap.view.mapValues { perf =>
          Json
            .obj(
              "rating" -> perf.intRating.value
            )
            .add("prov" -> perf.glicko.provisional)
        }.toMap
      )
