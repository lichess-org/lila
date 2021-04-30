package lila.swiss

import play.api.i18n.Lang

import lila.i18n.{ I18nKeys => trans }
import lila.rating.PerfType
import lila.user.{ Title, User }

sealed trait SwissCondition {

  def name(perf: PerfType)(implicit lang: Lang): String

  def withVerdict(verdict: SwissCondition.Verdict) = SwissCondition.WithVerdict(this, verdict)
}

object SwissCondition {

  trait FlatCond {

    def apply(user: User, perf: PerfType): SwissCondition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean, val reason: Option[Lang => String])
  case object Accepted                        extends Verdict(true, none)
  case class Refused(because: Lang => String) extends Verdict(false, because.some)

  case class WithVerdict(condition: SwissCondition, verdict: Verdict)

  case object Titled extends SwissCondition with FlatCond {
    def name(perf: PerfType)(implicit lang: Lang) = "Only titled players"
    def apply(user: User, perf: PerfType) =
      if (user.title.exists(_ != Title.LM)) Accepted
      else Refused(name(perf)(_))
  }

  case class NbRatedGame(nb: Int) extends SwissCondition with FlatCond {

    def apply(user: User, perf: PerfType) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).nb >= nb) Accepted
      else
        Refused { implicit lang =>
          val missing = nb - user.perfs(perf).nb
          trans.needNbMorePerfGames.pluralTxt(missing, missing, perf.trans)
        }

    def name(perf: PerfType)(implicit lang: Lang) =
      trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, perf.trans)
  }

  case class MaxRating(rating: Int) extends SwissCondition {

    def apply(perf: PerfType, getMaxRating: GetMaxRating)(
        user: User
    )(implicit ec: scala.concurrent.ExecutionContext): Fu[Verdict] =
      if (user.perfs(perf).provisional) fuccess(Refused { implicit lang =>
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      })
      else if (user.perfs(perf).intRating > rating) fuccess(Refused { implicit lang =>
        trans.yourPerfRatingIsTooHigh.txt(perf.trans, user.perfs(perf).intRating)
      })
      else
        getMaxRating(perf) map {
          case r if r <= rating => Accepted
          case r =>
            Refused { implicit lang =>
              trans.yourTopWeeklyPerfRatingIsTooHigh.txt(perf.trans, r)
            }
        }

    def maybe(user: User, perf: PerfType): Boolean =
      !user.perfs(perf).provisional && user.perfs(perf).intRating <= rating

    def name(perf: PerfType)(implicit lang: Lang) = trans.ratedLessThanInPerf.txt(rating, perf.trans)
  }

  case class MinRating(rating: Int) extends SwissCondition with FlatCond {

    def apply(user: User, perf: PerfType) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).provisional) Refused { implicit lang =>
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      }
      else if (user.perfs(perf).intRating < rating) Refused { implicit lang =>
        trans.yourPerfRatingIsTooLow.txt(perf.trans, user.perfs(perf).intRating)
      }
      else Accepted

    def name(perf: PerfType)(implicit lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, perf.trans)
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type]
  ) {

    lazy val list: List[SwissCondition] = List(nbRatedGame, maxRating, minRating, titled).flatten

    def relevant = list.nonEmpty

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(
        perf: PerfType,
        getMaxRating: GetMaxRating
    )(user: User)(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating => c(perf, getMaxRating)(user) map c.withVerdict
        case c: FlatCond  => fuccess(c withVerdict c(user, perf))
      }.sequenceFu dmap All.WithVerdicts

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def similar(other: All) = sameRatings(other) && titled == other.titled

    def isRatingLimited = maxRating.isDefined || minRating.isDefined
  }

  object All {
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      titled = none
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal {
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)
    }
  }

  final class Verify(historyApi: lila.history.HistoryApi) {

    def apply(swiss: Swiss, user: User)(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      swiss.settings.conditions.withVerdicts(swiss.perfType, getMaxRating)(user)
    }
  }

  object BSONHandlers {
    import reactivemongo.api.bson._
    import lila.db.dsl._
    implicit private val NbRatedGameHandler = Macros.handler[NbRatedGame]
    implicit private val MaxRatingHandler   = Macros.handler[MaxRating]
    implicit private val MinRatingHandler   = Macros.handler[MinRating]
    implicit private val TitledHandler = quickHandler[Titled.type](
      { case _: BSONValue => Titled },
      _ => BSONBoolean(true)
    )
    implicit val AllBSONHandler = Macros.handler[All]
  }

//   object JSONHandlers {
//     import play.api.libs.json._

//     def verdictsFor(verdicts: All.WithVerdicts, perf: PerfType, lang: Lang) =
//       Json.obj(
//         "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
//           Json.obj(
//             "condition" -> cond.name(perf)(lang),
//             "verdict" -> (verd match {
//               case Refused(reason) => reason(lang)
//               case Accepted        => JsString("ok")
//             })
//           )
//         },
//         "accepted" -> verdicts.accepted
//       )
//   }

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._
    val perfAuto = "auto" -> "Auto"
    val perfKeys = "auto" :: PerfType.nonPuzzle.map(_.key)
    def perfChoices(implicit lang: Lang) =
      perfAuto :: PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    val nbRatedGames = Vector(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, _) => (0, "No restriction")
      case x      => x
    }
    val nbRatedGame = mapping(
      "nb" -> number(min = 0, max = ~nbRatedGames.lastOption)
    )(NbRatedGame.apply)(NbRatedGame.unapply)
    case class RatingSetup(rating: Option[Int]) {
      def actualRating = rating.filter(r => r > 600 && r < 3000)
    }
    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "rating" -> optional(numberIn(maxRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)
    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "rating" -> optional(numberIn(minRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)
    def all =
      mapping(
        "nbRatedGame" -> optional(nbRatedGame),
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> optional(boolean)
      )(AllSetup.apply)(AllSetup.unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGame],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        titled: Option[Boolean]
    ) {

      def validRatings =
        (minRating.actualRating, maxRating.actualRating) match {
          case (Some(min), Some(max)) => min < max
          case _                      => true
        }

      def all = All(
        nbRatedGame.filter(_.nb > 0),
        maxRating.actualRating map MaxRating,
        minRating.actualRating map MinRating,
        ~titled option Titled
      )
    }
    object AllSetup {
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none),
        minRating = RatingSetup(none),
        titled = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame,
          maxRating = RatingSetup(all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.rating)),
          titled = all.titled has Titled option true
        )
    }
  }
}
