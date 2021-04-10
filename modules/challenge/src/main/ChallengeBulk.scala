package lila.challenge

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import reactivemongo.api.bson.Macros
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.LilaStream
import lila.common.Template
import lila.db.dsl._
import lila.game.{ Game, Player }
import lila.hub.actorApi.map.TellMany
import lila.hub.DuctSequencers
import lila.rating.PerfType
import lila.setup.SetupBulk.{ ScheduledBulk, ScheduledGame }
import lila.user.User

final class ChallengeBulkApi(
    colls: ChallengeColls,
    msgApi: ChallengeMsg,
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
  implicit private val messageHandler = stringAnyValHandler[Template](_.value, Template.apply)
  implicit private val bulkHandler    = Macros.handler[ScheduledBulk]

  private val coll = colls.bulk

  private val workQueue =
    new DuctSequencers(maxSize = 16, expiration = 10 minutes, timeout = 10 seconds, name = "challenge.bulk")

  def scheduledBy(me: User): Fu[List[ScheduledBulk]] =
    coll.list[ScheduledBulk]($doc("by" -> me.id))

  def deleteBy(id: String, me: User): Fu[Boolean] =
    coll.delete.one($doc("_id" -> id, "by" -> me.id)).map(_.n == 1)

  def startClocks(id: String, me: User): Fu[Boolean] =
    coll
      .updateField($doc("_id" -> id, "by" -> me.id, "pairedAt" $exists true), "startClocksAt", DateTime.now)
      .map(_.n == 1)

  def schedule(bulk: ScheduledBulk): Fu[Either[String, ScheduledBulk]] = workQueue(bulk.by) {
    coll.list[ScheduledBulk]($doc("by" -> bulk.by, "pairedAt" $exists false)) flatMap { bulks =>
      val nbGames = bulks.map(_.games.size).sum
      if (bulks.sizeIs >= 10) fuccess(Left("Already too many bulks queued"))
      else if (bulks.map(_.games.size).sum >= 1000) fuccess(Left("Already too many games queued"))
      else if (bulks.exists(_ collidesWith bulk))
        fuccess(Left("A bulk containing the same players is scheduled at the same time"))
      else coll.insert.one(bulk) inject Right(bulk)
    }
  }

  private[challenge] def tick: Funit =
    checkForPairing >> checkForClocks

  private def checkForPairing: Funit =
    coll.one[ScheduledBulk]($doc("pairAt" $lte DateTime.now, "pairedAt" $exists false)) flatMap {
      _ ?? { bulk =>
        workQueue(bulk.by) {
          makePairings(bulk).void
        }
      }
    }

  private def checkForClocks: Funit =
    coll.one[ScheduledBulk]($doc("startClocksAt" $lte DateTime.now, "pairedAt" $exists true)) flatMap {
      _ ?? { bulk =>
        workQueue(bulk.by) {
          startClocksNow(bulk)
        }
      }
    }

  private def startClocksNow(bulk: ScheduledBulk): Funit = {
    Bus.publish(TellMany(bulk.games.map(_.id), lila.round.actorApi.round.StartClock), "roundSocket")
    coll.delete.one($id(bulk._id)).void
  }

  private def makePairings(bulk: ScheduledBulk): Funit = {
    val perfType = PerfType(bulk.variant, Speed(bulk.clock))
    Source(bulk.games)
      .mapAsyncUnordered(8) { game =>
        userRepo.pair(game.white, game.black) map2 { case (white, black) =>
          (game.id, white, black)
        }
      }
      .mapConcat(_.toList)
      .map { case (id, white, black) =>
        val game = Game
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
        (game, white, black)
      }
      .mapAsyncUnordered(8) { case (game, white, black) =>
        gameRepo.insertDenormalized(game) >>- onStart(game.id) inject {
          (game, white, black)
        }
      }
      .mapAsyncUnordered(8) { case (game, white, black) =>
        msgApi.onApiPair(game.id, white.light, black.light)(bulk.by, bulk.message)
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
