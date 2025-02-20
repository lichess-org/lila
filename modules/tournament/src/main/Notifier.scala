package lila.tournament

import scala.concurrent.duration._

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.notify.Notification
import lila.notify.Notification.Notifies
import lila.notify.NotifyApi

final private class Notifier(
    tourRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    notifyApi: NotifyApi,
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
) {

  def apply(days: Notifier.Days) =
    tourRepo.coll
      .find(daySelecor(days), $doc("_id" -> true, "startsAt" -> true).some)
      .cursor[Notifier.Tour]()
      .documentSource(Int.MaxValue)
      .mapAsync(1) { tour =>
        tourRepo.setNotifiedTime(tour._id, DateTime.now) >>
          tourNotifications(tour)
      }
      .mapConcat(identity)
      .grouped(5)
      .throttle(1, 500 millis)
      .mapAsync(1)(ns => notifyApi.addNotifications(ns.toList))
      .toMat(Sink.ignore)(Keep.right)
      .run()
      .void

  private def daySelecor(days: Notifier.Days) = {
    val now = DateTime.now()
    days match {
      case Notifier.OneDay =>
        $doc(
          "startsAt" $lt now.plusDays(1),
          "startsAt" $gt now.plusHours(
            12, // no need to notify of tours that start so soon
          ),
          $or(
            "notified" $exists false,
            "notified" $lt now.minusDays(2),
          ),
        )
      case Notifier.Week =>
        $doc(
          "startsAt" $lt now.plusDays(7),
          "startsAt" $gt now.plusDays(
            5, // max 5 days before to not overlap with one day
          ),
          "notified" $exists false,
        )
    }
  }

  private def tourNotifications(tour: Notifier.Tour): Fu[List[Notification]] =
    playerRepo
      .allByTour(tour._id)
      .map { players =>
        players.map(player =>
          Notification.make(
            Notifies(player.userId),
            lila.notify.TournamentReminder(
              id = tour._id,
              date = tour.startsAt,
            ),
          ),
        )
      }
}

object Notifier {
  sealed trait Days
  case object OneDay extends Days
  case object Week   extends Days

  case class Tour(_id: String, startsAt: DateTime)

  implicit lazy val tourHandler: BSONDocumentHandler[Tour] = Macros.handler[Tour]

}
