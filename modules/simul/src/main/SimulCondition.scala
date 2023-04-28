package lila.simul

import play.api.i18n.Lang

import lila.i18n.I18nKeys as trans
import lila.rating.{ Perf, PerfType }
import lila.user.{ Title, User }
import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*

object SimulCondition:

  case class All(
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      teamMember: Option[TeamMember]
  ) extends ConditionList(List(maxRating, minRating, teamMember)):

    def verify(user: User, pt: PerfType, getUserTeams: GetUserTeams, getMaxRating: GetMaxRating)(using
        Executor
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user, pt) map c.withVerdict
        case c: MinRating  => fuccess(c(user, pt).withVerdict)
        case c: TeamMember => c(user, getUserTeams) map c.withVerdict
      }.parallel dmap All.WithVerdicts.apply

  object All:
    val empty = All(
      maxRating = none,
      minRating = none,
      teamMember = none
    )

    case class WithVerdicts(list: Seq[WithVerdict]) extends AnyVal:
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }
    import lila.rating.BSONHandlers.perfTypeKeyHandler
    private given BSONDocumentHandler[MaxRating]  = Macros.handler
    private given BSONDocumentHandler[MinRating]  = Macros.handler
    private given BSONDocumentHandler[TeamMember] = Macros.handler
    given BSONDocumentHandler[All]                = Macros.handler

  object DataForm:
    import play.api.data.Forms.*
    import lila.common.Form.{ *, given }
    val perfKeys = PerfType.nonPuzzle.map(_.key)
    def perfChoices(using Lang) =
      PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    case class RatingSetup(perf: Option[Perf.Key], rating: Option[IntRating]):
      def convert[A](f: (PerfType, IntRating) => A): Option[A] = for
        perf     <- perf
        perfType <- PerfType(perf)
        rating   <- rating
      yield f(perfType, rating)

    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
      "rating" -> optional(numberIn(maxRatings).into[IntRating])
    )(RatingSetup.apply)(unapply)
    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
      "rating" -> optional(numberIn(minRatings).into[IntRating])
    )(RatingSetup.apply)(unapply)
    def all(teams: Set[TeamId]) =
      mapping(
        "maxRating" -> maxRating,
        "minRating" -> minRating,
        "team"      -> optional(of[TeamId].verifying(teams.contains))
      )(AllSetup.apply)(unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        maxRating: RatingSetup,
        minRating: RatingSetup,
        team: Option[TeamId]
    ):

      def validRatings =
        (minRating.perf, maxRating.perf) match
          case (Some(min_pref), Some(max_pref)) if min_pref == max_pref =>
            (minRating.rating, maxRating.rating) match
              case (Some(min), Some(max)) => min < max
              case _                      => true
          case _ => true

      def convert(teams: Set[(TeamId, TeamName)]) = All(
        maxRating.convert(MaxRating.apply),
        minRating.convert(MinRating.apply),
        team flatMap { id => teams.find(_._1 == id) } map TeamMember.apply.tupled
      )

    object AllSetup:
      val default = AllSetup(
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none),
        team = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating)),
          team = all.teamMember.map(_.teamId)
        )
