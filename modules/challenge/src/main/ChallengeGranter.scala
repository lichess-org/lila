package lila.challenge

import play.api.i18n.Lang

import lila.i18n.I18nKeys.{ challenge as trans }
import lila.pref.Pref
import lila.rating.PerfType
import lila.relation.{ Block, Follow }
import lila.user.User

case class ChallengeDenied(dest: User, reason: ChallengeDenied.Reason)

object ChallengeDenied:

  enum Reason:
    case YouAreAnon
    case YouAreBlocked
    case TheyDontAcceptChallenges
    case RatingOutsideRange(perf: PerfType)
    case RatingIsProvisional(perf: PerfType)
    case FriendsOnly
    case BotUltraBullet
    case SelfChallenge

  def translated(d: ChallengeDenied)(using Lang): String =
    d.reason match
      case Reason.YouAreAnon               => trans.registerToSendChallenges.txt()
      case Reason.YouAreBlocked            => trans.youCannotChallengeX.txt(d.dest.titleUsername)
      case Reason.TheyDontAcceptChallenges => trans.xDoesNotAcceptChallenges.txt(d.dest.titleUsername)
      case Reason.RatingOutsideRange(perf) =>
        trans.yourXRatingIsTooFarFromY.txt(perf.trans, d.dest.titleUsername)
      case Reason.RatingIsProvisional(perf) => trans.cannotChallengeDueToProvisionalXRating.txt(perf.trans)
      case Reason.FriendsOnly    => trans.xOnlyAcceptsChallengesFromFriends.txt(d.dest.titleUsername)
      case Reason.BotUltraBullet => "Bots cannot play UltraBullet. Choose a slower time control."
      case Reason.SelfChallenge  => "You cannot challenge yourself."

final class ChallengeGranter(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
):

  import ChallengeDenied.Reason.*

  val ratingThreshold = 300

  def isDenied(fromOption: Option[User], dest: User, perfType: Option[PerfType])(using
      ec: Executor
  ): Fu[Option[ChallengeDenied]] =
    fromOption
      .fold[Fu[Option[ChallengeDenied.Reason]]] {
        prefApi.get(dest).map(_.challenge) map {
          case Pref.Challenge.ALWAYS => none
          case _                     => YouAreAnon.some
        }
      } { from =>
        relationApi.fetchRelation(dest, from) zip
          prefApi.get(dest).map(_.challenge) map {
            case (Some(Block), _)                                  => YouAreBlocked.some
            case (_, Pref.Challenge.NEVER)                         => TheyDontAcceptChallenges.some
            case (Some(Follow), _)                                 => none // always accept from followed
            case (_, _) if from.marks.engine && !dest.marks.engine => YouAreBlocked.some
            case (_, Pref.Challenge.FRIEND)                        => FriendsOnly.some
            case (_, Pref.Challenge.RATING) =>
              perfType.so: pt =>
                if (from.perfs(pt).provisional || dest.perfs(pt).provisional) RatingIsProvisional(pt).some
                else
                  val diff = math.abs(from.perfs(pt).intRating.value - dest.perfs(pt).intRating.value)
                  (diff > ratingThreshold) option RatingOutsideRange(pt)
            case (_, Pref.Challenge.REGISTERED) => none
            case _ if from == dest              => SelfChallenge.some
            case _                              => none
          }
      }
      .map {
        case None if dest.isBot && perfType.has(PerfType.UltraBullet) => BotUltraBullet.some
        case res                                                      => res
      }
      .map {
        _.map { ChallengeDenied(dest, _) }
      }
