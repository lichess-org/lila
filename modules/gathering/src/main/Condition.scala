package lila.gathering

import play.api.i18n.Lang

import lila.i18n.I18nKeys as trans
import lila.rating.PerfType
import lila.user.{ Title, User, Me, UserPerfs }
import lila.hub.LightTeam.TeamName
import lila.rating.Perf

trait Condition:

  def name(perf: PerfType)(using Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)

object Condition:

  trait FlatCond:
    def apply(perf: PerfType)(using Me, Perf): Condition.Verdict

  type GetMaxRating = PerfType => Fu[IntRating]
  type GetMyTeamIds = Me => Fu[List[TeamId]]

  enum Verdict(val accepted: Boolean, val reason: Option[Lang => String]):
    case Accepted                         extends Verdict(true, none)
    case Refused(because: Lang => String) extends Verdict(false, because.some)
    case RefusedUntil(until: Instant)     extends Verdict(false, none)
  export Verdict.*

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond:
    def name(pt: PerfType)(using Lang) = "Only titled players"
    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if me.title.exists(_ != Title.LM) && me.noBot
      then Accepted
      else Refused(name(pt)(using _))

  case class NbRatedGame(nb: Int) extends Condition with FlatCond:
    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if me.hasTitle then Accepted
      else if perf.nb >= nb then Accepted
      else
        Refused: lang =>
          val missing = nb - perf.nb
          trans.needNbMorePerfGames.pluralTxt(missing, missing, pt.trans(using lang))(using lang)
    def name(pt: PerfType)(using Lang) =
      trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, pt.trans)

  abstract trait RatingCondition:
    val rating: IntRating

  case class MaxRating(rating: IntRating) extends Condition with RatingCondition:

    def apply(
        pt: PerfType
    )(using perf: Perf, getMaxRating: GetMaxRating)(using Me, Executor): Fu[Verdict] =
      if perf.provisional.yes
      then
        fuccess(Refused: lang =>
          trans.yourPerfRatingIsProvisional.txt(pt.trans(using lang))(using lang))
      else if perf.intRating > rating
      then
        fuccess(Refused: lang =>
          trans.yourPerfRatingIsTooHigh.txt(pt.trans(using lang), perf.intRating)(using lang))
      else
        getMaxRating(pt).map:
          case r if r <= rating => Accepted
          case r =>
            Refused: lang =>
              trans.yourTopWeeklyPerfRatingIsTooHigh.txt(pt.trans(using lang), r)(using lang)

    def maybe(pt: PerfType)(using me: Me, perf: Perf): Boolean =
      perf.provisional.no && perf.intRating <= rating

    def name(pt: PerfType)(using lang: Lang) = trans.ratedLessThanInPerf.txt(rating, pt.trans)

  case class MinRating(rating: IntRating) extends Condition with RatingCondition with FlatCond:

    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if perf.provisional.yes then
        Refused: lang =>
          trans.yourPerfRatingIsProvisional.txt(pt.trans(using lang))(using lang)
      else if perf.intRating < rating then
        Refused: lang =>
          trans.yourPerfRatingIsTooLow.txt(pt.trans(using lang), perf.intRating)(using lang)
      else Accepted

    def name(pt: PerfType)(using Lang) = trans.ratedMoreThanInPerf.txt(rating, pt.trans)

  case class TeamMember(teamId: TeamId, teamName: TeamName) extends Condition:
    def name(pt: PerfType)(using lang: Lang) = trans.mustBeInTeam.txt(teamName)
    def apply(using getMyTeamIds: Me => Fu[List[TeamId]], me: Me)(using Executor) =
      getMyTeamIds(me).map: userTeamIds =>
        if userTeamIds contains teamId then Accepted
        else
          Refused: lang =>
            trans.youAreNotInTeam.txt(teamName)(using lang)

  case class AllowList(value: String) extends Condition with FlatCond:
    private lazy val segments: Set[String] = value.linesIterator.map(_.trim.toLowerCase).toSet
    private val titled                     = "%titled"
    private def allowAnyTitledUser         = segments contains titled
    def apply(pt: PerfType)(using me: Me, perf: Perf): Condition.Verdict =
      if segments.contains(me.userId.value) then Accepted
      else if allowAnyTitledUser && me.hasTitle then Accepted
      else Refused { _ => "Your name is not in the tournament line-up." }
    def userIds: Set[UserId]           = UserId.from(segments - titled)
    def name(pt: PerfType)(using Lang) = "Fixed line-up"

  case class WithVerdicts(list: List[WithVerdict]):
    export list.nonEmpty
    def accepted = list.forall(_.verdict.accepted)
