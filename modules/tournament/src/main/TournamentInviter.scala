package lidraughts.tournament

import lidraughts.user.User

import akka.actor._
import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.notify.{ Notification, NotifyApi, LimitedTournamentInvitation }
import lidraughts.rating.PerfType

private final class TournamentInviter private (
    api: TournamentApi,
    notifyApi: NotifyApi
) extends Actor {

  import TournamentInviter._

  def receive = {

    case User.Active(user) if qualifies(user) =>
      notifyApi.exists(Notification.Notifies(user.id), $doc("content.type" -> "u")) flatMap {
        case true => funit
        case false => notifyApi addNotificationWithoutSkipOrEvent Notification.make(
          Notification.Notifies(user.id),
          LimitedTournamentInvitation
        )
      }
  }

  def qualifies(user: User) =
    !user.seenRecently &&
      !user.kid &&
      user.count.rated > 50 &&
      user.toints < 10 &&
      bestRating(user).??(1700 >=) &&
      firstTime(user)

  def firstTime(user: User) =
    if (notifiedCache get user.id) false
    else {
      notifiedCache put user.id
      true
    }

  val notifiedCache = new lidraughts.memo.ExpireSetMemo(1 hour)
}

object TournamentInviter {

  val minGames = 0
  val perfs = List(PerfType.Blitz, PerfType.Rapid)

  def bestRating(user: User) = user.perfs.bestRatingInWithMinGames(perfs, minGames)

  def findNextFor(
    user: User,
    tours: VisibleTournaments,
    canEnter: Tournament => Fu[Boolean]
  ): Fu[Option[Tournament]] = bestRating(user) match {
    case None => fuccess(none)
    case Some(rating) if rating > 2000 => fuccess(none)
    case Some(rating) => lidraughts.common.Future.find(tours.unfinished.filter { t =>
      t.conditions.maxRating.??(_.rating >= rating)
    }.take(4))(canEnter)
  }

  def start(system: ActorSystem, api: TournamentApi, notifyApi: NotifyApi) = {
    val ref = system.actorOf(Props(new TournamentInviter(api, notifyApi)))
    system.lidraughtsBus.subscribe(ref, 'userActive)
  }
}
