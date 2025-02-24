package lila.tournament

import akka.stream.scaladsl.*
import io.lettuce.core.RedisClient
import play.api.libs.json.*
import scalalib.cache.{ ExpireSetMemo, FrequencyThreshold }

import lila.common.Json.given
import lila.common.{ LilaScheduler, LilaStream }
import lila.core.chess.Rank

final class TournamentLilaHttp(
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    cached: TournamentCache,
    duelStore: DuelStore,
    statsApi: TournamentStatsApi,
    jsonView: JsonView,
    pause: Pause,
    lightUserApi: lila.core.user.LightUserApi,
    redisClient: RedisClient
)(using akka.stream.Materializer, Scheduler, Executor):

  def handles(tour: Tournament) = isOnLilaHttp.get(tour.id)
  private def handledIds        = isOnLilaHttp.keys
  def hit(tour: Tournament) =
    if tour.nbPlayers > 10 && !tour.isFinished && hitCounter(tour.id)
    then isOnLilaHttp.put(tour.id)

  private val isOnLilaHttp = ExpireSetMemo[TourId](3.hours)
  private val hitCounter   = FrequencyThreshold[TourId](10, 20.seconds)

  private val channel = "http-out"
  private val conn    = redisClient.connectPubSub()

  LilaScheduler("TournamentLilaHttp", _.Every(1.second), _.AtMost(30.seconds), _.Delay(14.seconds)):
    tournamentRepo
      .idsCursor(handledIds)
      .documentSource()
      .mapAsyncUnordered(4): tour =>
        if tour.finishedSinceSeconds.exists(_ > 20) then isOnLilaHttp.remove(tour.id)
        arenaFullJson(tour).map(Json.stringify)
      .map: str =>
        lila.mon.tournament.lilaHttp.fullSize.record(str.size)
        conn.async.publish(channel, str)
      .runWith(LilaStream.sinkCount)
      .monSuccess(_.tournament.lilaHttp.tick)
      .addEffect(lila.mon.tournament.lilaHttp.nbTours.update(_))
      .void

  private def arenaFullJson(tour: Tournament): Fu[JsObject] = for
    data  <- jsonView.cachableData.get(tour.id)
    stats <- statsApi(tour)
    teamStanding <- tour.isTeamBattle.soFu:
      jsonView.fetchAndRenderTeamStandingJson(TeamBattle.maxTeams)(tour.id)
    fullStanding <- playerRepo
      .sortedCursor(tour.id, 100, _.pri)
      .documentSource()
      .zipWithIndex
      .mapAsync(16): (player, index) =>
        for
          sheet <- cached.sheet(tour, player.userId)
          ranked = RankedPlayer(Rank(index.toInt + 1), player)
          json <- playerJson(tour, sheet, ranked)
        yield json
      .runWith(Sink.seq)
      .map(JsArray(_))
  yield jsonView.commonTournamentJson(tour, data, stats, teamStanding) ++ Json
    .obj(
      "id" -> tour.id,
      "ongoingUserGames" -> {
        duelStore
          .get(tour.id)
          .so[String]:
            _.map(d => s"${d.p1.name.id}&${d.p2.name.id}/${d.gameId}").mkString(",")
      },
      "standing" -> fullStanding
    )
    .add("noStreak" -> tour.noStreak)

  private def playerJson(
      tour: Tournament,
      sheet: arena.Sheet,
      rankedPlayer: RankedPlayer
  )(using Executor): Fu[JsObject] =
    val p = rankedPlayer.player
    lightUserApi
      .asyncFallback(p.userId)
      .map: light =>
        Json
          .obj(
            "name"   -> light.name,
            "rating" -> p.rating,
            "score"  -> p.score,
            "sheet"  -> sheet.scoresToString
          )
          .add("title" -> light.title)
          .add("flair" -> light.flair)
          .add("provisional" -> p.provisional)
          .add("withdraw" -> p.withdraw)
          .add("team" -> p.team)
          .add("fire" -> p.fire)
          .add("pause" -> p.withdraw.so(pause.remainingDelay(p.userId, tour)))
