package lila.tournament

import lila.user.User

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi, LimitedTournamentInvitation }
import lila.rating.PerfType

private final class TournamentNotifier private (
    api: TournamentApi,
    notifyApi: NotifyApi) extends Actor {

  import TournamentNotifier._

  val minGames = 20
  val maxRating = 1700
  val perfs = List(PerfType.Blitz, PerfType.Classical)

  def receive = {

    case User.Active(user) if qualifies(user) =>
      notifyApi.exists(Notification.Notifies(user.id), $doc("content.type" -> "u")) flatMap {
        case true => funit
        case false => notifyApi addNotification Notification(
          Notification.Notifies(user.id),
          LimitedTournamentInvitation)
      }
  }

  def qualifies(user: User) = {
    println(user.seenRecently, user.id)
    println {
      !user.seenRecently &&
        !user.kid &&
        user.count.rated > 50 &&
        user.toints < 100 &&
        user.perfs.bestRatingInWithMinGames(perfs, minGames).??(maxRating >=) &&
        !alreadyNotified(user)
    }
    !user.seenRecently
  }

  def alreadyNotified(user: User) =
    if (notifiedCache get user.id) false
    else {
      notifiedCache put user.id
      true
    }

  val notifiedCache = new lila.memo.ExpireSetMemo(1 hour)
}

private object TournamentNotifier {

  def start(system: ActorSystem, api: TournamentApi, notifyApi: NotifyApi) = {
    val ref = system.actorOf(Props(new TournamentNotifier(api, notifyApi)))
    system.lilaBus.subscribe(ref, 'userActive)
  }
}
