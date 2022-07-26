package lila.tournament

import akka.stream.scaladsl._
import com.softwaremill.tagging._
import io.lettuce.core.RedisClient
import play.api.libs.json._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaScheduler, LilaStream }
import lila.memo.SettingStore
import lila.memo.{ ExpireSetMemo, FrequencyThreshold }

final class TournamentLilaHttp(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    cached: Cached,
    duelStore: DuelStore,
    statsApi: TournamentStatsApi,
    jsonView: JsonView,
    pause: Pause,
    lightUserApi: lila.user.LightUserApi,
    redisClient: RedisClient
)(implicit mat: akka.stream.Materializer, scheduler: akka.actor.Scheduler, ec: ExecutionContext) {

  def handles(tour: Tournament) = isOnLilaHttp get tour.id
  def handledIds                = isOnLilaHttp.keys
  def hit(tour: Tournament) =
    if (tour.nbPlayers > 10 && !tour.isFinished && hitCounter(tour.id)) isOnLilaHttp.put(tour.id)

  private val isOnLilaHttp = new ExpireSetMemo(3 hours)
  private val hitCounter   = new FrequencyThreshold[Tournament.ID](10, 20 seconds)

  private val channel = "http-out"
  private val conn    = redisClient.connectPubSub()

  LilaScheduler(_.Every(1 second), _.AtMost(30 seconds), _.Delay(14 seconds)) {
    tournamentRepo
      .idsCursor(handledIds)
      .documentSource()
      .mapAsyncUnordered(4) { tour =>
        if (tour.finishedSinceSeconds.exists(_ > 20)) isOnLilaHttp.remove(tour.id)
        arenaFullJson(tour)
      }
      .map { json =>
        val str = Json stringify json
        lila.mon.tournament.lilaHttp.fullSize.record(str.size)
        conn.async.publish(channel, str).unit
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .monSuccess(_.tournament.lilaHttp.tick)
      .addEffect(lila.mon.tournament.lilaHttp.nbTours.update(_).unit)
      .void
  }

  private def arenaFullJson(tour: Tournament): Fu[JsObject] = for {
    data  <- jsonView.cachableData get tour.id
    stats <- statsApi(tour)
    teamStanding <- tour.isTeamBattle ?? jsonView
      .fetchAndRenderTeamStandingJson(TeamBattle.maxTeams)(tour.id)
      .dmap(some)
    fullStanding <- playerRepo
      .sortedCursor(tour.id, 100, ReadPreference.primary)
      .documentSource()
      .zipWithIndex
      .mapAsync(16) { case (player, index) =>
        for {
          sheet <- cached.sheet(tour, player.userId)
          json <- playerJson(
            tour,
            sheet,
            RankedPlayer(index.toInt + 1, player),
            streakable = tour.streakable
          )
        } yield json
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(JsArray(_))

  } yield jsonView.commonTournamentJson(tour, data, stats, teamStanding) ++ Json
    .obj(
      "id" -> tour.id,
      "ongoingUserGames" -> duelStore
        .get(tour.id)
        .?? { _.map(d => s"${d.p1.name.id}&${d.p2.name.id}/${d.gameId}").mkString(",") },
      "standing" -> fullStanding
    )
    .add("noStreak" -> tour.noStreak)

  private def playerJson(
      tour: Tournament,
      sheet: arena.Sheet,
      rankedPlayer: RankedPlayer,
      streakable: Boolean
  )(implicit ec: ExecutionContext): Fu[JsObject] = {
    val p = rankedPlayer.player
    lightUserApi async p.userId map { light =>
      Json
        .obj(
          "name"   -> light.fold(p.userId)(_.name),
          "rating" -> p.rating,
          "score"  -> p.score,
          "sheet"  -> JsonView.scoresToString(sheet)
        )
        .add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
        .add("team" -> p.team)
        .add("fire" -> p.fire)
        .add("pause" -> {
          p.withdraw ?? pause.remainingDelay(p.userId, tour).map(_.seconds)
        })
    }
  }
}
