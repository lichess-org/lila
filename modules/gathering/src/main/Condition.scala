package lila.gathering

import play.api.i18n.Lang

import lila.i18n.I18nKeys as trans
import lila.rating.PerfType
import lila.user.{ Title, User }
import lila.hub.LightTeam.TeamName

sealed trait Condition:

  def name(using Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)

object Condition:

  trait FlatCond:

    def apply(user: User): Condition.Verdict

  type GetMaxRating   = PerfType => Fu[IntRating]
  type GetUserTeamIds = User => Fu[List[TeamId]]

  sealed abstract class Verdict(val accepted: Boolean, val reason: Option[Lang => String])
  case object Accepted                        extends Verdict(true, none)
  case class Refused(because: Lang => String) extends Verdict(false, because.some)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond:
    def name(using Lang) = "Only titled players"
    def apply(user: User) =
      if (user.title.exists(_ != Title.LM) && user.noBot) Accepted
      else Refused(l => name(using l))

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond:

    def apply(user: User) =
      if user.hasTitle then Accepted
      else
        perf match
          case Some(p) if user.perfs(p).nb >= nb => Accepted
          case Some(p) =>
            Refused: lang =>
              val missing = nb - user.perfs(p).nb
              trans.needNbMorePerfGames.pluralTxt(missing, missing, p.trans(using lang))(using lang)
          case None if user.count.rated >= nb => Accepted
          case None =>
            Refused: lang =>
              val missing = nb - user.count.rated
              trans.needNbMoreGames.pluralSameTxt(missing)(using lang)

    def name(using Lang) =
      perf match
        case None    => trans.moreThanNbRatedGames.pluralSameTxt(nb)
        case Some(p) => trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, p.trans)

  abstract trait RatingCondition:
    val perf: PerfType
    val rating: IntRating

  case class MaxRating(perf: PerfType, rating: IntRating) extends Condition with RatingCondition:

    def apply(
        getMaxRating: GetMaxRating
    )(user: User)(using Executor): Fu[Verdict] =
      if (user.perfs(perf).provisional.yes) fuccess(Refused { lang =>
        trans.yourPerfRatingIsProvisional.txt(perf.trans(using lang))(using lang)
      })
      else if (user.perfs(perf).intRating > rating) fuccess(Refused { lang =>
        trans.yourPerfRatingIsTooHigh.txt(perf.trans(using lang), user.perfs(perf).intRating)(using lang)
      })
      else
        getMaxRating(perf) map {
          case r if r <= rating => Accepted
          case r =>
            Refused { lang =>
              trans.yourTopWeeklyPerfRatingIsTooHigh.txt(perf.trans(using lang), r)(using lang)
            }
        }

    def maybe(user: User): Boolean =
      user.perfs(perf).provisional.no && user.perfs(perf).intRating <= rating

    def name(using lang: Lang) = trans.ratedLessThanInPerf.txt(rating, perf.trans)

  case class MinRating(perf: PerfType, rating: IntRating)
      extends Condition
      with RatingCondition
      with FlatCond:

    def apply(user: User) =
      if (user.perfs(perf).provisional.yes) Refused { lang =>
        trans.yourPerfRatingIsProvisional.txt(perf.trans(using lang))(using lang)
      }
      else if (user.perfs(perf).intRating < rating) Refused { lang =>
        trans.yourPerfRatingIsTooLow.txt(perf.trans(using lang), user.perfs(perf).intRating)(using lang)
      }
      else Accepted

    def name(using lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, perf.trans)

  case class TeamMember(teamId: TeamId, teamName: TeamName) extends Condition:
    def name(using lang: Lang) = trans.mustBeInTeam.txt(teamName)
    def apply(user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ) =
      getUserTeamIds(user) map { userTeamIds =>
        if (userTeamIds contains teamId) Accepted
        else
          Refused { lang =>
            trans.youAreNotInTeam.txt(teamName)(using lang)
          }
      }

  case class AllowList(value: String) extends Condition with FlatCond:

    private lazy val segments = value.linesIterator.map(_.trim.toLowerCase).toSet

    private def allowAnyTitledUser = segments contains "%titled"

    def apply(user: User): Condition.Verdict =
      if (segments contains user.id.value) Accepted
      else if (allowAnyTitledUser && user.hasTitle) Accepted
      else Refused { _ => "Your name is not in the tournament line-up." }

    def name(using lang: Lang) = "Fixed line-up"

  case class WithVerdicts(list: List[WithVerdict]) extends AnyVal:
    def relevant = list.nonEmpty
    def accepted = list.forall(_.verdict.accepted)
