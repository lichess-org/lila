package lila.challenge

import play.api.i18n.Lang

import lila.i18n.I18nKeys
import lila.pref.Pref
import lila.rating.PerfType
import lila.relation.{ Block, Follow }
import lila.user.User

case class ChallengeDenied(dest: User, reason: ChallengeDenied.Reason)

object ChallengeDenied {

  sealed trait Reason

  object Reason {
    case object YouAreAnon                         extends Reason
    case object YouAreBlocked                      extends Reason
    case object TheyDontAcceptChallenges           extends Reason
    case class RatingOutsideRange(perf: PerfType)  extends Reason
    case class RatingIsProvisional(perf: PerfType) extends Reason
    case object FriendsOnly                        extends Reason
    case object BotUltraBullet                     extends Reason
  }

  def translated(d: ChallengeDenied)(implicit lang: Lang): String =
    d.reason match {
      case Reason.YouAreAnon               => I18nKeys.registerToSendChallenges.txt()
      case Reason.YouAreBlocked            => I18nKeys.youCannotChallengeX.txt(d.dest.titleUsername)
      case Reason.TheyDontAcceptChallenges => I18nKeys.xDoesNotAcceptChallenges.txt(d.dest.titleUsername)
      case Reason.RatingOutsideRange(perf) =>
        I18nKeys.yourXRatingIsTooFarFromY.txt(perf.trans, d.dest.titleUsername)
      case Reason.RatingIsProvisional(perf) => I18nKeys.cannotChallengeDueToProvisionalXRating.txt(perf.trans)
      case Reason.FriendsOnly               => I18nKeys.xOnlyAcceptsChallengesFromFriends.txt(d.dest.titleUsername)
      case Reason.BotUltraBullet            => "Bots cannot play UltraBullet. Choose a slower time control."
    }
}

final class ChallengeGranter(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
) {

  import ChallengeDenied.Reason._

  val ratingThreshold = 300

  def apply(fromOption: Option[User], dest: User, perfType: Option[PerfType])(implicit
      ec: scala.concurrent.ExecutionContext
  ): Fu[Option[ChallengeDenied]] =
    fromOption
      .fold[Fu[Option[ChallengeDenied.Reason]]](fuccess(YouAreAnon.some)) { from =>
        relationApi.fetchRelation(dest, from) zip
          prefApi.getPref(dest).map(_.challenge) map {
            case (Some(Block), _)                                  => YouAreBlocked.some
            case (_, Pref.Challenge.NEVER)                         => TheyDontAcceptChallenges.some
            case (Some(Follow), _)                                 => none // always accept from followed
            case (_, _) if from.marks.engine && !dest.marks.engine => YouAreBlocked.some
            case (_, Pref.Challenge.FRIEND)                        => FriendsOnly.some
            case (_, Pref.Challenge.RATING) =>
              perfType ?? { pt =>
                if (from.perfs(pt).provisional || dest.perfs(pt).provisional)
                  RatingIsProvisional(pt).some
                else {
                  val diff = math.abs(from.perfs(pt).intRating - dest.perfs(pt).intRating)
                  (diff > ratingThreshold) option RatingOutsideRange(pt)
                }
              }
            case (_, Pref.Challenge.ALWAYS) => none
            case _                          => none
          }
      }
      .map {
        case None if dest.isBot && perfType.has(PerfType.UltraBullet) => BotUltraBullet.some
        case res                                                      => res
      }
      .map {
        _.map { ChallengeDenied(dest, _) }
      }

}
