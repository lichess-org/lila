package lila.api

import akka.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.RequestHeader

import lila.common.{ Bus, HTTPRequest }
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }
import lila.core.net.{ UserAgent, Origin }
import lila.oauth.AccessToken

final class GameStreamByOauthOrigin(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    tokenApi: lila.oauth.AccessTokenApi,
    lightUserGet: lila.core.LightUser.GetterSync
)(using akka.stream.Materializer, Executor):

  private val streamUserId = UserId.t3
  private val origin = Origin("https://auth.taketaketake.com")

  private def mon = lila.mon.game.streamByOauthOrigin

  private type MutableUserSet = collection.mutable.Set[UserId]
  private val tokenUsersFu: Fu[MutableUserSet] =
    tokenApi.userIdsByClientOrigin(origin).map(_.to(collection.mutable.Set))

  Bus.sub[AccessToken.Create]: tc =>
    if tc.token.clientOrigin.has(origin) then
      tokenUsersFu.foreach: us =>
        us.add(tc.token.userId)
        mon.users("newToken").update(us.size)

  def apply(since: Option[Instant], extraUsers: Set[UserId])(using
      me: Me,
      req: RequestHeader
  ): Either[String, Source[JsValue, ?]] =
    for
      _ <-
        if me.userId == streamUserId then Right(()) else Left("Invalid authenticated user")
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
        tokenUsers <- tokenUsersFu
        _ = extraUsers.foreach(tokenUsers.add)
        recentlySeenUsers <- userRepo.filterSeenSince((since | nowInstant).minusMinutes(20))(tokenUsers)
      yield run(since, ua, tokenUsers, recentlySeenUsers, logMsg)

  private def run(
      since: Option[Instant],
      ua: UserAgent,
      tokenUsers: MutableUserSet,
      recentlySeenUsers: List[UserId],
      logMsg: String
  ): Source[JsObject, ?] =
    var nbGames = 0
    val startedAt = nowInstant
    val startStream =
      Source.queue[Game](300, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
        streams.open(ua)
        logger.branch("gameStream").info(s"OPEN  $logMsg")
        mon.users("initial").update(tokenUsers.size)
        mon.users("recentlySeen").update(recentlySeenUsers.size)

        def matches(game: Game) = game.nonAi &&
          game.players.exists(_.userId.exists(tokenUsers))

        val subStart = Bus.sub[StartGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        val subFinish = Bus.sub[FinishGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsub[StartGame](subStart)
            Bus.unsub[FinishGame](subFinish)
            streams.close(ua)
            val seconds = nowSeconds - startedAt.toSeconds
            logger.branch("gameStream").info(s"CLOSE $logMsg ($seconds seconds, $nbGames games)")
      }
    pastGamesSource(recentlySeenUsers, since)
      .concat(currentGamesSource(recentlySeenUsers))
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
