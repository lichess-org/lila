package lila.challenge

import play.api.i18n.Lang

import lila.i18n.I18nKeys.{ challenge as trans }
import lila.pref.Pref
import lila.rating.PerfType
import lila.relation.{ Block, Follow }
import lila.user.{ User, Me }
import scala.util.Try
import lila.user.UserPerfs
import lila.rating.Perf
import lila.Lila

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
    perfsRepo: lila.user.UserPerfsRepo,
    relationApi: lila.relation.RelationApi
):

  import ChallengeDenied.Reason.*

  val ratingThreshold = 300

  def isDenied(dest: User, perfType: PerfType)(using
      Executor
  )(using me: Option[Me]): Fu[Option[ChallengeDenied]] = me
    .fold[Fu[Option[ChallengeDenied.Reason]]] {
      prefApi.get(dest).map(_.challenge) map {
        case Pref.Challenge.ALWAYS => none
        case _                     => YouAreAnon.some
      }
    } { from =>
      type Res = Option[ChallengeDenied.Reason]
      given Conversion[Res, Fu[Res]] = fuccess
      relationApi.fetchRelation(dest, from) zip
        prefApi.get(dest).map(_.challenge) flatMap {
          case (Some(Block), _)                                  => YouAreBlocked.some
          case (_, Pref.Challenge.NEVER)                         => TheyDontAcceptChallenges.some
          case (Some(Follow), _)                                 => none // always accept from followed
          case (_, _) if from.marks.engine && !dest.marks.engine => YouAreBlocked.some
          case (_, Pref.Challenge.FRIEND)                        => FriendsOnly.some
          case (_, Pref.Challenge.RATING) =>
            perfsRepo
              .perfsOf(from.value -> dest, _.sec)
              .map: (fromPerfs, destPerfs) =>
                if fromPerfs(perfType).provisional || destPerfs(perfType).provisional
                then RatingIsProvisional(perfType).some
                else
                  val diff =
                    math.abs(fromPerfs(perfType).intRating.value - destPerfs(perfType).intRating.value)
                  (diff > ratingThreshold) option RatingOutsideRange(perfType)
          case (_, Pref.Challenge.REGISTERED) => none
          case _ if from == dest              => SelfChallenge.some
          case _                              => none
        }
    }
    .map:
      case None if dest.isBot && perfType == PerfType.UltraBullet => BotUltraBullet.some
      case res                                                    => res
    .map:
      _.map { ChallengeDenied(dest, _) }
