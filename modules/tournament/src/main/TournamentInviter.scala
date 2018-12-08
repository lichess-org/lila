package lidraughts.tournament

import lidraughts.user.User

import scala.concurrent.duration._

import lidraughts.common.Tellable
import lidraughts.db.dsl._
import lidraughts.notify.{ Notification, NotifyApi, LimitedTournamentInvitation }
import lidraughts.rating.PerfType

private final class TournamentInviter(
    api: TournamentApi,
    notifyApi: NotifyApi
) extends Tellable with Tellable.PartialReceive with Tellable.HashCode {

  import TournamentInviter._

  val receive: Tellable.Receive = {
    case User.Active(user) if qualifies(user) =>
      notifyApi.exists(Notification.Notifies(user.id), $doc("content.type" -> "u")) flatMap {
        case true => funit
        case false => notifyApi addNotificationWithoutSkipOrEvent Notification.make(
          Notification.Notifies(user.id),
          LimitedTournamentInvitation
        )
      }
  }

  private def qualifies(user: User) = false
  /* Disabled until there actually are rating limited tournaments in the schedule
      !user.seenRecently &&
      !user.kid &&
      user.count.rated > 50 &&
      user.toints < 10 &&
      bestRating(user).??(1700 >=) &&
      firstTime(user)*/

  private def firstTime(user: User) =
    if (notifiedCache get user.id) false
    else {
      notifiedCache put user.id
      true
    }

  private val notifiedCache = new lidraughts.memo.ExpireSetMemo(1 hour)
}

object TournamentInviter {

  private val minGames = 0
  private val perfs = List(PerfType.Blitz, PerfType.Rapid)

  private def bestRating(user: User) = user.perfs.bestRatingInWithMinGames(perfs, minGames)

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

  def start(bus: lidraughts.common.Bus, api: TournamentApi, notifyApi: NotifyApi) =
    bus.subscribe(new TournamentInviter(api, notifyApi), 'userActive)
}
