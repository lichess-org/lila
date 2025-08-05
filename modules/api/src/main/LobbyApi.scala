package lila.api

import play.api.libs.json.{ JsObject, Json, Writes }

import lila.common.Json.given
import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.lobby.{ LobbySocket, SeekApi }
import lila.rating.UserPerfsExt.perfsList

final class LobbyApi(
    lightUserApi: lila.user.LightUserApi,
    seekApi: SeekApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    gameJson: lila.game.JsonView,
    lobbySocket: LobbySocket
)(using Executor):

  def apply(using me: Option[UserWithPerfs]): Fu[(JsObject, List[Pov])] =
    me.foldUse(seekApi.forAnon)(seekApi.forMe)
      .mon(_.lobby.segment("seeks"))
      .zip(me.so(gameProxyRepo.urgentGames).mon(_.lobby.segment("urgentGames")))
      .flatMap { (seeks, povs) =>
        val displayedPovs = povs.take(9)
        lightUserApi
          .preloadMany(displayedPovs.flatMap(_.opponent.userId))
          .inject(
            Json
              .obj(
                "seeks" -> seeks.map(_.render),
                "nowPlaying" -> displayedPovs.map(nowPlaying),
                "nbNowPlaying" -> povs.size,
                "nbMyTurn" -> povs.count(_.isMyTurn),
                "counters" -> Json.obj(
                  "members" -> lobbySocket.counters.members,
                  "rounds" -> lobbySocket.counters.rounds
                )
              )
              .add("ratingMap", me.map(_.perfs).map(ratingMap))
              .add(
                "me",
                me.map: u =>
                  Json.obj("username" -> u.username).add("isBot" -> u.isBot)
              ) -> displayedPovs
          )
      }

  def nowPlaying(pov: Pov): JsObject = gameJson.ownerPreview(pov)(using lightUserApi.sync)

  private def ratingMap(perfs: UserPerfs): JsObject =
    Writes
      .keyMapWrites[PerfKey, JsObject, Map]
      .writes(
        perfs.perfsList.view.map { (pk, perf) =>
          pk -> Json
            .obj("rating" -> perf.intRating.value)
            .add("prov" -> perf.glicko.provisional)
        }.toMap
      )
