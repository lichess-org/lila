package lila.fishnet

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime

import lila.hub.{ actorApi => hubApi }
import makeTimeout.short

private final class MoveDB(
    system: ActorSystem
) {

  import Work.Move

  def add(move: Move) = actor ! Add(move)

  def acquire(client: Client): Fu[Option[Move]] =
    actor ? Acquire(client) mapTo manifest[Option[Move]]

  def postResult(
    moveId: Work.Id,
    client: Client,
    data: JsonApi.Request.PostMove,
    measurement: lila.mon.Measurement
  ) =
    actor ! PostResult(moveId, client, data, measurement)

  def monitor = actor ! Mon

  def clean = actor ? Clean mapTo manifest[Iterable[Move]]

  private object Mon
  private object Clean
  private case class Add(move: Move)
  private case class Acquire(client: Client)
  private case class PostResult(
      moveId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostMove,
      measurement: lila.mon.Measurement
  )

  private val actor = system.actorOf(Props(new Actor {

    val coll = scala.collection.mutable.Map.empty[Work.Id, Move]

    val maxSize = 300

    def receive = {

      case Add(move) if !coll.exists(_._2 similar move) => coll += (move.id -> move)

      case Mon =>
        import Client.Skill.Move.key
        lila.mon.fishnet.work.moveDbSize(coll.size)
        lila.mon.fishnet.work.queued(key)(coll.count(_._2.nonAcquired))
        lila.mon.fishnet.work.acquired(key)(coll.count(_._2.isAcquired))

      case Clean =>
        val since = DateTime.now minusSeconds 3
        val timedOut = coll.values.filter(_ acquiredBefore since)
        if (timedOut.nonEmpty) logger.debug(s"cleaning ${timedOut.size} of ${coll.size} moves")
        timedOut.foreach { m => updateOrGiveUp(m.timeout) }
        sender ! timedOut

      case Add(move) =>
        clearIfFull
        coll += (move.id -> move)

      case Acquire(client) => sender ! coll.values.foldLeft(none[Move]) {
        case (found, m) if m.nonAcquired => Some {
          found.fold(m) { a =>
            if (m.canAcquire(client) && m.createdAt.isBefore(a.createdAt)) m else a
          }
        }
        case (found, _) => found
      }.map { m =>
        val move = m assignTo client
        coll += (move.id -> move)
        move
      }

      case PostResult(moveId, client, data, measurement) =>
        coll get moveId match {
          case None => Monitor.notFound(moveId, client)
          case Some(move) if move isAcquiredBy client => data.move.uci match {
            case Some(uci) =>
              coll -= move.id
              Monitor.move(move, client)
              system.lilaBus.publish(
                hubApi.map.Tell(move.game.id, hubApi.round.FishnetPlay(uci, move.currentFen)),
                'roundMapTell
              )
            case _ =>
              updateOrGiveUp(move.invalid)
              Monitor.failure(move, client, lila.base.LilaException("Missing move"))
          }
          case Some(move) => Monitor.notAcquired(move, client)
        }
        measurement.finish()
    }

    def updateOrGiveUp(move: Move) =
      if (move.isOutOfTries) {
        logger.warn(s"Give up on move $move")
        coll -= move.id
      } else coll += (move.id -> move)

    def clearIfFull =
      if (coll.size > maxSize) {
        logger.warn(s"MoveDB collection is full! maxSize=$maxSize. Dropping all now!")
        lila.mon.fishnet.move.dbDrop()
        coll.clear()
      }
  }))
}
