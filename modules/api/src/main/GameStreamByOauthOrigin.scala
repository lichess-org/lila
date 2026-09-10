package lila.api

import org.apache.pekko.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import bloomfilter.mutable.BloomFilter
import scalalib.net.UserAgent

import lila.common.{ Bus, HTTPRequest }
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }
import lila.core.net.Origin
import lila.oauth.AccessToken

final class GameStreamByOauthOrigin(
    gameRepo: lila.game.GameRepo,
    tokenApi: lila.oauth.AccessTokenApi,
    lightUserGet: lila.core.LightUser.GetterSync
)(using org.apache.pekko.stream.Materializer, Executor):

  private case class Client(
      user: UserId,
      origin: Origin,
      estimatedCount: Int,
      seenSince: FiniteDuration,
      startEvents: Boolean
  ):
    val mon = lila.mon.game.StreamByOauthOrigin(origin)
    var population = 0
    def incPopulation(): Unit =
      population = population + 1
      mon.users("newToken").update(population)

  private val allowedClients = List(
    Client(UserId.t3, Origin("https://auth.taketaketake.com"), 70_000, 365.days, true),
    Client(UserId("marcusbuffett"), Origin("https://chessbook.com"), 15_000, 90.days, false)
  )
  private val falsePositiveRate = 0.0005 // 0.05% false positives

  private type MutableUserSet = BloomFilter[String]
  private val tokenUsersFu: Map[Origin, Fu[MutableUserSet]] =
    allowedClients
      .map: client =>
        val bloom = BloomFilter[String](client.estimatedCount, falsePositiveRate)
        client.origin -> tokenApi
          .userIdsByClientOrigin(client.origin, client.seenSince)
          .runWith:
            Sink.fold[Int, UserId](0): (counter, userId) =>
              bloom.add(userId.value)
              counter + 1
          .addEffect: nb =>
            client.population = nb
          .inject(bloom)
      .toMap

  Bus.sub[AccessToken.Create]: tc =>
    for
      tokenOrigin <- tc.token.clientOrigin
      usersFu <- tokenUsersFu.get(tokenOrigin)
      client <- allowedClients.find(_.origin == tokenOrigin)
    do
      usersFu.foreach: users =>
        users.add(tc.token.userId.value)
        client.incPopulation()

  def apply(since: Option[Instant], extraUsers: Set[UserId])(using
      me: Me,
      req: RequestHeader
  ): Either[String, Source[JsValue, ?]] =
    for
      client <- allowedClients.find(_.user.is(me)).toRight("Invalid authenticated user")
      origin = client.origin
      since <- since match
        case Some(s) if s.isAfter(nowInstant) => Left("`since` is in the future")
        case Some(s) if s.isBefore(nowInstant.minusHours(3)) => Left("`since` is older than 3 hours")
        case s => Right(s)
      myTokenUsersFu <- tokenUsersFu.get(origin).toRight("No token users for this origin")
      ip = HTTPRequest.ipAddress(req)
      ua = HTTPRequest.userAgent(req)
      request = s"$ip ${req.uri} $ua"
      logMsg = s"$origin $request ${since.so(_.toNow.toMinutes)}m"
    yield Source.futureSource:
      for
        tokenUsers <- myTokenUsersFu
        _ = extraUsers.foreach(u => tokenUsers.add(u.value))
        recentlySeenUsers <- tokenApi
          .recentlySeenUserIdsByClientOrigin(
            origin,
            (since | nowInstant).minusMinutes(20)
          )
      yield run(client, since, ua, tokenUsers, recentlySeenUsers, logMsg)

  private def run(
      client: Client,
      since: Option[Instant],
      ua: UserAgent,
      tokenUsers: MutableUserSet,
      recentlySeenUsers: List[UserId],
      logMsg: String
  ): Source[JsObject, ?] =
    var nbGames = 0
    val startedAt = nowInstant
    val startStream = Source
      .queue[Game](300, org.apache.pekko.stream.OverflowStrategy.dropHead)
      .mapMaterializedValue: queue =>
        streams.open(client, ua)
        lila.log.system.info(s"gameStream OPEN  $logMsg")
        client.mon.users("recentlySeen").update(recentlySeenUsers.size)

        def matches(game: Game) = game.nonAi &&
          game.players.exists(_.userId.exists(id => tokenUsers.mightContain(id.value)))

        val subStart = client.startEvents.option:
          Bus.sub[StartGame]: e =>
            if matches(e.game) then queue.offer(e.game)

        val subFinish = Bus.sub[FinishGame]: e =>
          if matches(e.game) then queue.offer(e.game)

        queue
          .watchCompletion()
          .addEffectAnyway:
            subStart.foreach(Bus.unsub)
            Bus.unsub(subFinish)
            streams.close(client, ua)
            val seconds = nowSeconds - startedAt.toSeconds
            lila.log.system.info(s"gameStream CLOSE $logMsg ($seconds seconds, $nbGames games)")

    pastGamesSource(recentlySeenUsers, since)
      .concat(if client.startEvents then currentGamesSource(recentlySeenUsers) else Source.empty)
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map: wif =>
        client.mon.event(if wif.game.finished then "finish" else "start").increment()
        nbGames = nbGames + 1
        toJson(wif)

  private def toJson(wif: WithInitialFen): JsObject =
    lila.game.GameStream.toJson(lightUserGet.some)(wif) ++
      wif.game.finished.so:
        Json.obj("moves" -> wif.game.sans.mkString(" "))

  private def pastGamesSource(userIds: Iterable[UserId], since: Option[Instant]): Source[Game, ?] =
    since.fold(Source.empty): since =>
      gameRepo.finishedByOneOfUserIdsSince(userIds, since).documentSource().throttle(100, 1.second)

  private def currentGamesSource(userIds: Iterable[UserId]): Source[Game, ?] =
    gameRepo.ongoingByOneOfUserIdsCursor(userIds).documentSource().throttle(100, 1.second)

  private object streams:
    private val count = scala.collection.mutable.Map[(Origin, UserAgent), Int]()
    private def inc(v: Int)(client: Client, ua: UserAgent) =
      val nb = count.updateWith(client.origin -> ua)(_.fold(v)(_ + v).atLeast(0).some) | 0
      client.mon.streams(ua).update(nb)
    def open = inc(1)
    def close = inc(-1)
