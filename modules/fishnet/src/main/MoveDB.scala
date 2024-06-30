package lila.fishnet

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.FishnetPlay
import lila.common.Bus

final class MoveDB(implicit system: ActorSystem) {

  import Work.Move

  implicit private val timeout = new akka.util.Timeout(20.seconds)

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
  ): Unit = {
    actor ! PostResult(workId, client, data)
  }

  def clean(): Fu[Iterable[Move]] = actor ? Clean mapTo manifest[Iterable[Move]]

  private object Clean
  private case class Add(move: Move)
  private case class Acquire(client: Client)
  private case class PostResult(
      moveId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostMove
  )

  private val actor = system.actorOf(Props(new Actor {

    val coll = scala.collection.mutable.Map.empty[Work.Id, Move]

    val maxSize = 300

    def receive = {

      case Add(move) =>
        clearIfFull()
        if (!coll.exists(_._2 similar move))
          coll += (move.id -> move)

      case Acquire(client) => {
        val now = DateTime.now
        sender() ! coll.values
          .filter(m =>
            m.nonAcquired &&
              m.canAcquire(client) &&
              (client.skill != Client.Skill.MoveStd || m.isStandard) &&
              m.delayMillis.fold(true) { millis =>
                now.isAfter(m.createdAt.plusMillis(millis))
              }
          )
          .minByOption(_.createdAt)
          .map { m =>
            val move = m assignTo client
            coll += (move.id -> move)
            move
          }
      }

      case PostResult(workId, client, data) => {
        coll get workId match {
          case None =>
            Monitor.notFound(workId, "move", client).unit
          case Some(move) if move isAcquiredBy client =>
            data.move.usi(move.game.variant) match {
              case Some(usi) =>
                coll -= move.id
                Monitor.move(client).unit
                Bus.publish(Tell(move.game.id, FishnetPlay(usi, move.ply)), "roundSocket")
              case _ =>
                sender() ! None
                updateOrGiveUp(move.invalid)
                Monitor.failure(move, client, new Exception("Missing move")).unit
            }
          case Some(move) =>
            sender() ! None
            Monitor.notAcquired(move, client).unit
        }
      }

      case Clean =>
        val since    = DateTime.now minusSeconds 5
        val timedOut = coll.values.filter(_ acquiredBefore since)
        if (timedOut.nonEmpty) logger.debug(s"cleaning ${timedOut.size} of ${coll.size} moves")
        timedOut.foreach { m =>
          logger.warn(s"Timeout move $m")
          updateOrGiveUp(m.timeout)
        }
    }

    def updateOrGiveUp(move: Move) =
      if (move.isOutOfTries) {
        logger.warn(s"Give up on move $move")
        coll -= move.id
      } else if (move.hasLastTry) {
        coll += (move.id -> move.sfenOnly)
      } else coll += (move.id -> move)

    def clearIfFull() =
      if (coll.sizeIs > maxSize) {
        logger.warn(s"MoveDB collection is full! maxSize=$maxSize. Dropping all now!")
        coll.clear()
      }
  }))

}
