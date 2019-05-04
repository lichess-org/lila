package lila.tournament

import lila.user.User

import scala.concurrent.duration._

import lila.common.Tellable
import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi, LimitedTournamentInvitation }
import lila.rating.PerfType

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

  private def qualifies(user: User) =
    !user.seenRecently &&
      !user.kid &&
      !user.hasTitle &&
      user.count.rated > 50 &&
      user.toints < 10 &&
      bestRating(user).??(1700 >=) &&
      firstTime(user)

  private def firstTime(user: User) =
    if (notifiedCache get user.id) false
    else {
      notifiedCache put user.id
      true
    }

  private val notifiedCache = new lila.memo.ExpireSetMemo(1 hour)
}

object TournamentInviter {

  private val minGames = 20
  private val perfs = List(PerfType.Blitz, PerfType.Rapid)

  private def bestRating(user: User) = user.perfs.bestRatingInWithMinGames(perfs, minGames)

  def findNextFor(
    user: User,
    tours: VisibleTournaments,
    canEnter: Tournament => Fu[Boolean]
  ): Fu[Option[Tournament]] = bestRating(user) match {
    case None => fuccess(none)
    case Some(rating) if rating > 2000 => fuccess(none)
    case Some(rating) => lila.common.Future.find(tours.unfinished.filter { t =>
      t.conditions.maxRating.??(_.rating >= rating)
    }.take(4))(canEnter)
  }

  def start(bus: lila.common.Bus, api: TournamentApi, notifyApi: NotifyApi) =
    bus.subscribe(new TournamentInviter(api, notifyApi), 'userActive)
}
