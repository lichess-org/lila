package lila.api

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }
import lila.oauth.AccessToken

final class GameStreamByOauthOrigin(
    gameRepo: lila.game.GameRepo,
    tokenApi: lila.oauth.AccessTokenApi,
    lightUserGet: lila.core.LightUser.GetterSync
)(using akka.stream.Materializer, Executor):

  private val origins = Map(
    UserId("taketaketakeapp") -> "https://auth.taketaketake.com",
    UserId("thibault") -> "http://127.0.0.1"
  )
  private def mon = lila.mon.game.streamByOauthOrigin

  def apply(since: Option[Instant])(using me: Me): Either[String, Source[JsValue, ?]] =
    for
      origin <- origins.get(me.userId).toRight("Invalid authenticated user")
      since <- since match
        case Some(s) if s.isAfter(nowInstant) => Left("`since` is in the future")
        case Some(s) if s.isBefore(nowInstant.minusHours(3)) => Left("`since` is older than 3 hours")
        case s => Right(s)
    yield Source.futureSource:
      tokenApi
        .userIdsByClientOrigin(origin)
        .map(run(since, origin, _))

  private def run(
      since: Option[Instant],
      origin: String,
      initialUserIds: Set[UserId]
  ): Source[JsObject, ?] =
    val startStream =
      Source.queue[Game](300, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
        var userIds = initialUserIds
        mon.users.update(initialUserIds.size)

        def matches(game: Game) = game.nonAi &&
          game.players.exists(_.userId.exists(userIds))

        val subStart = Bus.sub[StartGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        val subFinish = Bus.sub[FinishGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        val subToken = Bus.sub[AccessToken.Create]: tc =>
          if tc.token.clientOrigin.has(origin) then
            userIds = userIds + tc.token.userId
            mon.users.update(userIds.size)

        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsub[StartGame](subStart)
            Bus.unsub[FinishGame](subFinish)
            Bus.unsub[AccessToken.Create](subToken)
      }
    pastGamesSource(initialUserIds, since)
      .concat(currentGamesSource(initialUserIds))
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map: wif =>
        mon.event(if wif.game.finished then "finish" else "start").increment()
        toJson(wif)

  private def toJson(wif: WithInitialFen): JsObject =
    lila.game.GameStream.toJson(lightUserGet.some)(wif) ++ {
      wif.game.finished.so:
        Json.obj("moves" -> wif.game.sans.mkString(" "))
    }

  private def pastGamesSource(userIds: Set[UserId], since: Option[Instant]): Source[Game, ?] =
    since.fold(Source.empty): since =>
      gameRepo.finishedByOneOfUserIdsSince(userIds, since).documentSource().throttle(100, 1.second)

  private def currentGamesSource(userIds: Set[UserId]): Source[Game, ?] =
    gameRepo.ongoingByOneOfUserIdsCursor(userIds).documentSource().throttle(100, 1.second)
