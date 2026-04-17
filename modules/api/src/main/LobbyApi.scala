package lila.api

import play.api.libs.json.{ JsObject, Json, Writes }

import lila.common.Json.given
import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.lobby.LobbySocket
import lila.rating.UserPerfsExt.perfsList

final class LobbyApi(
    lightUserApi: lila.user.LightUserApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    gameJson: lila.game.JsonView,
    lobbySocket: LobbySocket
)(using Executor):

  def get(using me: Option[UserWithPerfs]): Fu[(JsObject, List[Pov])] =
    me.so(gameProxyRepo.urgentGames)
      .mon(_.lobby.segment("urgentGames"))
      .flatMap: povs =>
        val displayedPovs = povs.take(9)
        for _ <- lightUserApi.preloadMany(displayedPovs.flatMap(_.opponent.userId))
        yield Json
          .obj(
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

  def nowPlaying(pov: Pov): JsObject = gameJson.ownerPreview(pov)(using lightUserApi.sync)

  private def ratingMap(perfs: UserPerfs): JsObject =
    Writes
      .keyMapWrites[PerfKey, Int, Map]
      .writes(
        perfs.perfsList.view.map { (pk, perf) =>
          pk -> (perf.intRating.value * (if perf.glicko.provisional.yes then -1 else 1))
        }.toMap
      )
