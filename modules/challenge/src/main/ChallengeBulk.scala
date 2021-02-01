package lila.challenge

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import chess.variant.Variant
import chess.{ Clock, Mode, Situation, Speed }
import org.joda.time.DateTime
import reactivemongo.api.bson.Macros
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.LilaStream
import lila.db.dsl._
import lila.game.{ Game, Player }
import lila.hub.actorApi.map.TellMany
import lila.hub.DuctSequencers
import lila.rating.PerfType
import lila.setup.SetupBulk.{ ScheduledBulk, ScheduledGame }
import lila.user.User

final class ChallengeBulkApi(
    colls: ChallengeColls,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
    system: ActorSystem,
    mode: play.api.Mode
) {

  implicit private val gameHandler    = Macros.handler[ScheduledGame]
  implicit private val variantHandler = variantByKeyHandler
  implicit private val clockHandler   = clockConfigHandler
  implicit private val bulkHandler    = Macros.handler[ScheduledBulk]

  private val coll = colls.bulk

  private val workQueue =
    new DuctSequencers(maxSize = 64, expiration = 1 minute, timeout = 10 seconds, name = "challenge.bulk")

  def schedule(bulk: ScheduledBulk): Fu[Option[String]] = workQueue(bulk.by) {
    if (bulk.pairAt.isBeforeNow) makePairings(bulk) inject none
    else
      coll.list[ScheduledBulk]($doc("by" -> bulk.by, "pairedAt" $exists false)) flatMap { bulks =>
        val nbGames = bulks.map(_.games.size).sum
        if (bulks.sizeIs >= 10) fuccess("Already too many bulks queued".some)
        else if (bulks.map(_.games.size).sum >= 1000) fuccess("Already too many games queued".some)
        else if (bulks.exists(_ collidesWith bulk))
          fuccess("A bulk containing the same players is scheduled at the same time".some)
        else coll.insert.one(bulk) inject none
      }
  }

  private[challenge] def tick: Funit =
    checkForPairing >> checkForClocks

  private def checkForPairing: Funit =
    coll.one[ScheduledBulk]($doc("pairAt" $lte DateTime.now, "pairedAt" $exists false)) flatMap {
      _ ?? makePairings
    }

  private def checkForClocks: Funit =
    coll.one[ScheduledBulk]($doc("startClocksAt" $lte DateTime.now, "pairedAt" $exists true)) flatMap {
      _ ?? startClocks
    }

  private def startClocks(bulk: ScheduledBulk): Funit = workQueue(bulk.by) {
    Bus.publish(TellMany(bulk.games.map(_.id), lila.round.actorApi.round.StartClock), "roundSocket")
    coll.delete.one($id(bulk._id)).void
  }

  private def makePairings(bulk: ScheduledBulk): Funit = workQueue(bulk.by) {
    val perfType = PerfType(bulk.variant, Speed(bulk.clock))
    Source(bulk.games)
      .mapAsyncUnordered(8) { game =>
        userRepo.pair(game.white, game.black) map2 { case (white, black) =>
          (game.id, white, black)
        }
      }
      .mapConcat(_.toList)
      .map[Game] { case (id, white, black) =>
        Game
          .make(
            chess = chess.Game(situation = Situation(bulk.variant), clock = bulk.clock.toClock.some),
            whitePlayer = Player.make(chess.White, white.some, _(perfType)),
            blackPlayer = Player.make(chess.Black, black.some, _(perfType)),
            mode = bulk.mode,
            source = lila.game.Source.Api,
            pgnImport = None
          )
          .withId(id)
          .start
      }
      .mapAsyncUnordered(8) { game =>
        (gameRepo insertDenormalized game) >>- onStart(game.id)
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect { nb =>
        lila.mon.api.challenge.bulk.createNb(bulk.by).increment(nb).unit
      } >> {
      if (bulk.startClocksAt.isDefined)
        coll.updateField($id(bulk._id), "pairedAt", DateTime.now)
      else coll.delete.one($id(bulk._id))
    }.void
  }
}
