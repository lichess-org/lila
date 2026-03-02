package lila.api

import akka.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.RequestHeader

import lila.common.{ Bus, HTTPRequest }
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }
import lila.core.net.UserAgent
import lila.oauth.AccessToken

final class GameStreamByOauthOrigin(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    tokenApi: lila.oauth.AccessTokenApi,
    lightUserGet: lila.core.LightUser.GetterSync
)(using akka.stream.Materializer, Executor):

  private val origins = Map(
    UserId.ttt -> "https://auth.taketaketake.com"
  )
  private def mon = lila.mon.game.streamByOauthOrigin

  def apply(since: Option[Instant], extraUsers: Set[UserId])(using
      me: Me,
      req: RequestHeader
  ): Either[String, Source[JsValue, ?]] =
    for
      origin <- origins.get(me.userId).toRight("Invalid authenticated user")
      since <- since match
        case Some(s) if s.isAfter(nowInstant) => Left("`since` is in the future")
        case Some(s) if s.isBefore(nowInstant.minusHours(3)) => Left("`since` is older than 3 hours")
        case s => Right(s)
      randomName = ~scalalib.cuteName.CuteNameGenerator.make()
      ip = HTTPRequest.ipAddress(req)
      ua = HTTPRequest.userAgent(req)
      request = s"$ip ${req.uri} $ua"
      logMsg = s"$randomName $origin $request ${since.so(_.toNow.toMinutes)}m"
    yield Source.futureSource:
      for
        tokenUsers <- tokenApi.userIdsByClientOrigin(origin)
        allUsers = tokenUsers ++ extraUsers
        recentlySeenUsers <- userRepo.filterSeenSince((since | nowInstant).minusMinutes(30))(allUsers)
      yield run(since, origin, ua, allUsers, recentlySeenUsers, logMsg)

  private def run(
      since: Option[Instant],
      origin: String,
      ua: UserAgent,
      initialUserIds: Set[UserId],
      recentlySeenUserIds: List[UserId],
      logMsg: String
  ): Source[JsObject, ?] =
    var nbGames = 0
    val startedAt = nowInstant
    val startStream =
      Source.queue[Game](300, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
        var userIds = initialUserIds
        streams.open(ua)
        logger.branch("gameStream").info(s"OPEN  $logMsg")
        mon.users("initial").update(userIds.size)
        mon.users("recentlySeen").update(recentlySeenUserIds.size)

        def matches(game: Game) = game.nonAi &&
          game.players.exists(_.userId.exists(userIds))

        val subStart = Bus.sub[StartGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        val subFinish = Bus.sub[FinishGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        val subToken = Bus.sub[AccessToken.Create]: tc =>
          if tc.token.clientOrigin.has(origin) then
            userIds = userIds + tc.token.userId
            mon.users("newToken").update(userIds.size)

        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsub[StartGame](subStart)
            Bus.unsub[FinishGame](subFinish)
            Bus.unsub[AccessToken.Create](subToken)
            streams.close(ua)
            val seconds = nowSeconds - startedAt.toSeconds
            logger.branch("gameStream").info(s"CLOSE $logMsg ($seconds seconds, $nbGames games)")
      }
    pastGamesSource(recentlySeenUserIds, since)
      .concat(currentGamesSource(recentlySeenUserIds))
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map: wif =>
        mon.event(if wif.game.finished then "finish" else "start").increment()
        nbGames = nbGames + 1
        toJson(wif)

  private def toJson(wif: WithInitialFen): JsObject =
    lila.game.GameStream.toJson(lightUserGet.some)(wif) ++ {
      wif.game.finished.so:
        Json.obj("moves" -> wif.game.sans.mkString(" "))
    }

  private def pastGamesSource(userIds: Iterable[UserId], since: Option[Instant]): Source[Game, ?] =
    since.fold(Source.empty): since =>
      gameRepo.finishedByOneOfUserIdsSince(userIds, since).documentSource().throttle(100, 1.second)

  private def currentGamesSource(userIds: Iterable[UserId]): Source[Game, ?] =
    gameRepo.ongoingByOneOfUserIdsCursor(userIds).documentSource().throttle(100, 1.second)

  private object streams:
    private val count = scala.collection.mutable.Map[UserAgent, Int]()
    private def inc(v: Int)(ua: UserAgent) =
      val nb = count.updateWith(ua)(_.fold(v)(_ + v).atLeast(0).some) | 0
      mon.streams(ua).update(nb)
    def open = inc(1)
    def close = inc(-1)
