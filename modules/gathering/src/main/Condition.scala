package lila.gathering

import scalalib.model.Days
import chess.IntRating

import lila.core.LightUser.Me
import lila.core.i18n.{ I18nKey as trans, Translate }
import lila.core.team.LightTeam.TeamName
import lila.rating.PerfType

trait Condition:

  def name(perf: PerfType)(using Translate): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)

object Condition:

  trait FlatCond:
    def apply(perf: PerfType)(using Me, Perf): Condition.Verdict

  type GetMaxRating = PerfType => Fu[IntRating]
  type GetMyTeamIds = Me => Fu[List[TeamId]]
  type GetAge       = Me => Fu[Days]

  enum Verdict(val accepted: Boolean, val reason: Option[Translate => String]):
    case Accepted                              extends Verdict(true, none)
    case Refused(because: Translate => String) extends Verdict(false, because.some)
    case RefusedUntil(until: Instant)          extends Verdict(false, none)
  export Verdict.*

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond:
    def name(pt: PerfType)(using Translate) = trans.arena.onlyTitled.txt()
    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if me.title.exists(_.isFederation) then Accepted else Refused(name(pt)(using _))

  case class Bots(allowed: Boolean) extends Condition with FlatCond:
    def name(pt: PerfType)(using Translate) =
      if allowed then "Bot players are allowed"
      else "Bot players are not allowed"
    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if me.isBot && !allowed then Refused(name(pt)(using _)) else Accepted

  case class NbRatedGame(nb: Int) extends Condition with FlatCond:
    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if me.title.isDefined then Accepted
      else if perf.nb >= nb then Accepted
      else
        Refused: t =>
          given Translate = t
          val missing     = nb - perf.nb
          trans.site.needNbMorePerfGames.pluralTxt(missing, missing, pt.trans)
    def name(pt: PerfType)(using Translate) =
      trans.site.moreThanNbPerfRatedGames.pluralTxt(nb, nb, pt.trans)

  case class AccountAge(days: Days) extends Condition:
    def name(perf: PerfType)(using Translate): String =
      if days < 30 then s"${days.value} days old account"
      else if days < 365 then s"${days.value / 30} months old account"
      else s"${days.value / 365} years old account"
    def apply(using getAge: GetAge)(using Me, Executor): Fu[Verdict] =
      if summon[Me].title.isDefined then fuccess(Accepted)
      else
        getAge(summon[Me]).map:
          case age if age.value >= days.value => Accepted
          case age => Refused(_ => s"Your account is too young, by ${days.value - age.value} days.")

  abstract trait RatingCondition:
    val rating: IntRating

  case class MaxRating(rating: IntRating) extends Condition with RatingCondition:

    def apply(
        pt: PerfType
    )(using perf: Perf, getMaxRating: GetMaxRating)(using Me, Executor): Fu[Verdict] =
      if perf.provisional.yes
      then
        fuccess(Refused: t =>
          given Translate = t
          trans.site.yourPerfRatingIsProvisional.txt(pt.trans))
      else if perf.intRating > rating
      then
        fuccess(Refused: t =>
          given Translate = t
          trans.site.yourPerfRatingIsTooHigh.txt(pt.trans, perf.intRating))
      else
        getMaxRating(pt).map:
          case r if r <= rating => Accepted
          case r =>
            Refused: t =>
              given Translate = t
              trans.site.yourTopWeeklyPerfRatingIsTooHigh.txt(pt.trans, r)

    def maybe(pt: PerfType)(using me: Me, perf: Perf): Boolean =
      perf.provisional.no && perf.intRating <= rating

    def name(pt: PerfType)(using Translate) = trans.site.ratedLessThanInPerf.txt(rating, pt.trans)

  case class MinRating(rating: IntRating) extends Condition with RatingCondition with FlatCond:

    def apply(pt: PerfType)(using me: Me, perf: Perf) =
      if perf.provisional.yes then
        Refused: t =>
          given Translate = t
          trans.site.yourPerfRatingIsProvisional.txt(pt.trans)
      else if perf.intRating < rating then
        Refused: t =>
          given Translate = t
          trans.site.yourPerfRatingIsTooLow.txt(pt.trans, perf.intRating)
      else Accepted

    def name(pt: PerfType)(using Translate) = trans.site.ratedMoreThanInPerf.txt(rating, pt.trans)

  case class TeamMember(teamId: TeamId, teamName: TeamName) extends Condition:
    def name(pt: PerfType)(using Translate) = trans.site.mustBeInTeam.txt(teamName)
    def apply(using getMyTeamIds: Me => Fu[List[TeamId]], me: Me)(using Executor) =
      getMyTeamIds(me).map: userTeamIds =>
        if userTeamIds contains teamId then Accepted
        else
          Refused: t =>
            given Translate = t
            trans.site.youAreNotInTeam.txt(teamName)

  case class AllowList(value: String) extends Condition with FlatCond:
    private lazy val segments: Set[String] = value.linesIterator.map(_.trim.toLowerCase).toSet
    private val titled                     = "%titled"
    private def allowAnyTitledUser         = segments contains titled
    def apply(pt: PerfType)(using me: Me, perf: Perf): Condition.Verdict =
      if segments.contains(me.userId.value) then Accepted
      else if allowAnyTitledUser && me.title.isDefined then Accepted
      else Refused { _ => "Your name is not in the tournament line-up." }
    def userIds: Set[UserId]                = UserId.from(segments - titled)
    def name(pt: PerfType)(using Translate) = "Fixed line-up"

  case class WithVerdicts(list: List[WithVerdict]):
    def accepted = list.forall(_.verdict.accepted)
