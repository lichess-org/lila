package lila.challenge

import lila.pref.Pref
import lila.rating.PerfType
import lila.relation.{ Relation, Block, Follow }
import lila.user.User

case class ChallengeDenied(dest: User, reason: ChallengeDenied.Reason)

object ChallengeDenied {

  sealed trait Reason

  object Reason {
    case object YouAreAnon extends Reason
    case object YouAreBlocked extends Reason
    case object TheyDontAcceptChallenges extends Reason
    case class RatingOutsideRange(perf: PerfType) extends Reason
    case object FriendsOnly extends Reason
  }

  def inEnglish(d: ChallengeDenied) = d.reason match {
    case Reason.YouAreAnon => "Please register to send challenges"
    case Reason.YouAreBlocked => s"You cannot challenge ${d.dest.titleUsername}"
    case Reason.TheyDontAcceptChallenges => s"${d.dest.titleUsername} does not accept any challenge"
    case Reason.RatingOutsideRange(perf) => s"Your ${perf.name} rating is too far from ${d.dest.titleUsername} rating"
    case Reason.FriendsOnly => s"${d.dest.titleUsername} only accepts challenges from friends"
  }
}

final class ChallengeGranter(
    getPref: User => Fu[Pref],
    getRelation: (User, User) => Fu[Option[Relation]]
) {

  import ChallengeDenied.Reason._

  val ratingThreshold = 300

  def apply(fromOption: Option[User], dest: User, perfType: Option[PerfType]): Fu[Option[ChallengeDenied]] =
    fromOption.fold[Fu[Option[ChallengeDenied.Reason]]](fuccess(YouAreAnon.some)) { from =>
      getRelation(dest, from) zip getPref(dest).map(_.challenge) map {
        case (Some(Block), _) => YouAreBlocked.some
        case (_, Pref.Challenge.NEVER) => TheyDontAcceptChallenges.some
        case (Some(Follow), _) => none // always accept from followed
        case (_, _) if from.engine && !dest.engine => YouAreBlocked.some
        case (_, Pref.Challenge.FRIEND) => FriendsOnly.some
        case (_, Pref.Challenge.RATING) => perfType ?? { pt =>
          val diff = math.abs(from.perfs(pt).intRating - dest.perfs(pt).intRating)
          (diff > ratingThreshold) option RatingOutsideRange(pt)
        }
        case (_, Pref.Challenge.ALWAYS) => none
      }
    }.map {
      _.map { ChallengeDenied(dest, _) }
    }
}
