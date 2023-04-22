package lila.simul

import play.api.i18n.Lang

import lila.i18n.I18nKeys as trans
import lila.rating.{ Perf, PerfType }
import lila.user.{ Title, User }

sealed trait SimulCondition:
  def apply(user: User): SimulCondition.Verdict
  def name(using lang: Lang): String
  def withVerdict(verdict: SimulCondition.Verdict) = SimulCondition.WithVerdict(this, verdict)

object SimulCondition:

  enum Verdict(val accepted: Boolean, val reason: Option[Lang => String]):
    case Accepted                         extends Verdict(true, none)
    case Refused(because: Lang => String) extends Verdict(false, because.some)
  export Verdict.*

  case class WithVerdict(condition: SimulCondition, verdict: Verdict)

  case class MaxRating(perf: PerfType, rating: Int) extends SimulCondition:
    def apply(user: User) =
      if (user.perfs(perf).provisional.yes) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      }
      else if (user.perfs(perf).intRating > rating) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsTooHigh.txt(perf.trans, user.perfs(perf).intRating)
      }
      else Accepted

    def name(using lang: Lang) = trans.ratedLessThanInPerf.txt(rating, perf.trans)

  case class MinRating(perf: PerfType, rating: Int) extends SimulCondition:
    def apply(user: User) =
      if (user.perfs(perf).provisional.yes) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      }
      else if (user.perfs(perf).intRating < rating) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsTooLow.txt(perf.trans, user.perfs(perf).intRating)
      }
      else Accepted
    def name(using lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, perf.trans)

  case class All(
      maxRating: Option[MaxRating],
      minRating: Option[MinRating]
  ):
    lazy val list: List[SimulCondition] = List(maxRating, minRating).flatten

    def relevant = list.nonEmpty

    def withVerdicts(user: User)(using Executor): Fu[All.WithVerdicts] =
      list.map { c => fuccess(c withVerdict c(user)) }.parallel dmap All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def isRatingLimited = maxRating.isDefined || minRating.isDefined

  object All:
    val empty = All(
      maxRating = none,
      minRating = none
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal:
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)

  def verify(simul: Simul, user: Option[User])(using Executor): Fu[All.WithVerdicts] =
    user match
      case None       => fuccess(simul.conditions.accepted)
      case Some(user) => simul.conditions.withVerdicts(user)

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }
    import lila.rating.BSONHandlers.perfTypeKeyHandler
    private given BSONDocumentHandler[MaxRating] = Macros.handler
    private given BSONDocumentHandler[MinRating] = Macros.handler
    given BSONDocumentHandler[All]               = Macros.handler

  object DataForm:
    import play.api.data.Forms.*
    import lila.common.Form.{ *, given }
    val perfKeys = PerfType.nonPuzzle.map(_.key)
    def perfChoices(using lang: Lang) =
      PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    case class RatingSetup(perf: Option[Perf.Key], rating: Option[Int]):
      def convert[A](f: (PerfType, Int) => A): Option[A] =
        for {
          perf     <- perf
          perfType <- PerfType(perf)
          rating   <- rating
        } yield f(perfType, rating)

    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
      "rating" -> optional(numberIn(maxRatings))
    )(RatingSetup.apply)(unapply)
    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
      "rating" -> optional(numberIn(minRatings))
    )(RatingSetup.apply)(unapply)
    def all =
      mapping(
        "maxRating" -> maxRating,
        "minRating" -> minRating
      )(AllSetup.apply)(unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        maxRating: RatingSetup,
        minRating: RatingSetup
    ):

      def validRatings =
        (minRating.perf, maxRating.perf) match
          case (Some(min_pref), Some(max_pref)) if min_pref == max_pref =>
            (minRating.rating, maxRating.rating) match
              case (Some(min), Some(max)) => min < max
              case _                      => true
          case _ => true

      def all = All(
        maxRating.convert(MaxRating.apply),
        minRating.convert(MinRating.apply)
      )

    object AllSetup:
      val default = AllSetup(
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none)
      )
      def apply(all: All): AllSetup =
        AllSetup(
          maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating))
        )
