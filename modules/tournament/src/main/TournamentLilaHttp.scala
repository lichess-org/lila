package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import io.lettuce.core.RedisClient
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.LilaStream
import reactivemongo.api.ReadPreference

final private class TournamentLilaHttp(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    cached: Cached,
    duelStore: DuelStore,
    statsApi: TournamentStatsApi,
    jsonView: JsonView,
    lightUserApi: lila.user.LightUserApi,
    redisClient: RedisClient
)(implicit mat: akka.stream.Materializer, system: ActorSystem, ec: ExecutionContext) {

  private val channel = "http-out"
  private val conn    = redisClient.connectPubSub()

  private val periodicSender = system.actorOf(Props(new Actor {

    implicit def ec = context.dispatcher

    override def preStart(): Unit = {
      context setReceiveTimeout 20.seconds
      context.system.scheduler.scheduleOnce(10 seconds, self, Tick).unit
    }

    case object Tick

    def scheduleNext(): Unit =
      context.system.scheduler.scheduleOnce(1 second, self, Tick).unit

    def receive = {

      case ReceiveTimeout =>
        val msg = "tournament.lilaHttp timed out!"
        logger.branch("lila-http").error(msg)
        throw new RuntimeException(msg)

      case Tick =>
        tournamentRepo
          .startedCursorWithNbPlayersGte(1.some)
          .documentSource()
          .mapAsyncUnordered(4)(arenaFullJson)
          .map { json =>
            val str = Json stringify json
            lila.mon.tournament.lilaHttp.fullSize.record(str.size)
            conn.async.publish(channel, str).unit
          }
          .toMat(LilaStream.sinkCount)(Keep.right)
          .run()
          .monSuccess(_.tournament.lilaHttp.tick)
          .addEffectAnyway(scheduleNext())
          .unit
    }
  }))

  private def arenaFullJson(tour: Tournament): Fu[JsObject] = for {
    data         <- jsonView.cachableData get tour.id
    stats        <- statsApi(tour)
    teamStanding <- jsonView.getTeamStanding(tour)
    fullStanding <- playerRepo
      .sortedCursor(tour.id, 100, ReadPreference.primary)
      .documentSource(100)
      .zipWithIndex
      .mapAsync(16) { case (player, index) =>
        for {
          sheet <- cached.sheet(tour, player.userId)
          json <- playerJson(
            sheet,
            RankedPlayer(index.toInt + 1, player),
            streakable = tour.streakable
          )
        } yield json
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(JsArray(_))

  } yield jsonView.commonTournamentJson(tour, data, stats, teamStanding) ++ Json.obj(
    "id" -> tour.id,
    // TODO optimize as a single string
    "ongoingUserGames" -> duelStore.get(tour.id).fold(Json.obj()) { all =>
      JsObject(all.view.flatMap { duel =>
        duel.userIds.map { uid => uid -> JsString(duel.gameId) }
      }.toMap)
    },
    "standing" -> fullStanding
  )

  private def playerJson(
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
          "sheet"  -> sheet.scores.map(_.encoded)
        )
        .add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
        .add("team" -> p.team)
    }
  }
}
