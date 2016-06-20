package lila.tournament

import lila.user.User

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi, LimitedTournamentInvitation }
import lila.rating.PerfType

private final class TournamentInviter private (
    api: TournamentApi,
    notifyApi: NotifyApi) extends Actor {

  import TournamentInviter._

  def receive = {

    case User.Active(user) if qualifies(user) =>
      notifyApi.exists(Notification.Notifies(user.id), $doc("content.type" -> "u")) flatMap {
        case true => funit
        case false => notifyApi addNotification Notification(
          Notification.Notifies(user.id),
          LimitedTournamentInvitation)
      }
  }

  def qualifies(user: User) =
    !user.seenRecently &&
      !user.kid &&
      user.count.rated > 50 &&
      user.toints < 10 &&
      bestRating(user).??(1700 >=) &&
      !alreadyNotified(user)

  def alreadyNotified(user: User) =
    if (notifiedCache get user.id) false
    else {
      notifiedCache put user.id
      true
    }

  val notifiedCache = new lila.memo.ExpireSetMemo(1 hour)
}

object TournamentInviter {

  val minGames = 20
  val perfs = List(PerfType.Blitz, PerfType.Classical)

  def bestRating(user: User) = user.perfs.bestRatingInWithMinGames(perfs, minGames)

  def findNextFor(
    user: User,
    tours: VisibleTournaments,
    canEnter: Tournament => Fu[Boolean]): Fu[Option[Tournament]] = bestRating(user) match {
    case None                          => fuccess(none)
    case Some(rating) if rating > 2000 => fuccess(none)
    case Some(rating) => lila.common.Future.find(tours.unfinished.filter { t =>
      t.conditions.maxRating.??(_.rating >= rating)
    }.take(4))(canEnter)
  }

  def start(system: ActorSystem, api: TournamentApi, notifyApi: NotifyApi) = {
    val ref = system.actorOf(Props(new TournamentInviter(api, notifyApi)))
    system.lilaBus.subscribe(ref, 'userActive)
  }
}
