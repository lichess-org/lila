package lila.fishnet

import akka.actor._
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

import lila.hub.actorApi.map.{ Tell, TellAll }
import lila.hub.actorApi.round.{ FishnetPlay, FishnetStart }
import lila.common.{ Bus, Lilakka }

final class MoveDB(implicit system: ActorSystem) {

  import Work.Move

  implicit private val timeout = new akka.util.Timeout(20.seconds) //2

  def add(move: Move) = {
    actor ! Add(move)
  }

  def acquire(client: Client): Fu[Option[Move]] = {
    actor ? Acquire(client) mapTo manifest[Option[Move]]
  }

  def postResult(
      workId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostMove
  ) {
    actor ! PostResult(workId, client, data)
  }

  def clean = actor ? Clean mapTo manifest[Iterable[Move]]

  private object Mon
  private object Clean
  private case class Add(move: Move)
  private case class Acquire(client: Client)
  private case class PostResult(
      moveId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostMove,
  )

  private val actor = system.actorOf(Props(new Actor {

    val coll = scala.collection.mutable.Map.empty[Work.Id, Move]

    val maxSize = 300

    def receive = {

      case Add(move) if !coll.exists(_._2 similar move) => coll += (move.id -> move)

      case Add(move) =>
        clearIfFull()
        coll += (move.id -> move)

      case Acquire(client) => {
        sender() ! coll.values
          .foldLeft[Option[Move]](None) {
            case (found, m) if m.nonAcquired && (client.getVariants contains m.game.variant) => {
              Some {
                found.fold(m) { a =>
                  if (m.canAcquire(client) && m.createdAt.isBefore(a.createdAt)) m else a
                }
              }
            }
            case (found, _) => {
              found
            }
          }
          .map { m =>
            val move = m assignTo client
            coll += (move.id -> move)
            move
          }
        }
//Bus.publish(Tell(gameId, FishnetPlay(move, ply)), "roundSocket")
      case PostResult(workId, client, data) => {
        coll get workId match {
          case None =>
            Monitor.notFound(workId, client)
          case Some(move) if move isAcquiredBy client =>
            data.move.uci match {
              case Some(uci) =>
                coll -= move.id
                Monitor.move(move, client)
                Bus.publish(Tell(move.game.id, FishnetPlay(uci, move.game.ply)), "roundSocket")
              case _ =>
                sender() ! None
                updateOrGiveUp(move.invalid)
                Monitor.failure(move, client, new Exception("Missing move"))
            }
          case Some(move) =>
            sender() ! None
            Monitor.notAcquired(move, client)
        }
      }

      case Clean =>
        val since    = DateTime.now minusSeconds 3
        val timedOut = coll.values.filter(_ acquiredBefore since)
        if (timedOut.nonEmpty) logger.debug(s"cleaning ${timedOut.size} of ${coll.size} moves")
        timedOut.foreach { m =>
          logger.info(s"Timeout move $m")
          updateOrGiveUp(m.timeout)
        }
    }

    def updateOrGiveUp(move: Move) =
      if (move.isOutOfTries) {
        logger.warn(s"Give up on move $move")
        coll -= move.id
      } else coll += (move.id -> move)

    def clearIfFull() =
      if (coll.size > maxSize) {
        logger.warn(s"MoveDB collection is full! maxSize=$maxSize. Dropping all now!")
        coll.clear()
      }
  }))

}
