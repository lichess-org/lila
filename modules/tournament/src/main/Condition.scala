package lila.tournament

import play.api.i18n.Lang

import lila.hub.LightTeam._
import lila.i18n.{ I18nKeys => trans }
import lila.rating.PerfType
import lila.user.{ Title, User }

sealed trait Condition {

  def name(pt: PerfType)(implicit lang: Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)
}

object Condition {

  trait FlatCond {

    def apply(user: User, pt: PerfType): Condition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted                       extends Verdict(true)
  case class Refused(reason: Lang => String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond {
    def name(pt: PerfType)(implicit lang: Lang) = "Only titled players"
    def apply(user: User, pt: PerfType) =
      if (user.title.exists(_ != Title.LM) && user.noBot) Accepted
      else Refused(name(pt)(_))
  }

  case class NbRatedGame(nb: Int) extends Condition with FlatCond {

    def apply(user: User, pt: PerfType) =
      if (user.hasTitle || user.perfs(pt).nb >= nb) Accepted
      else
        Refused { implicit lang =>
          val missing = nb - user.perfs(pt).nb
          trans.needNbMorePerfGames.pluralTxt(missing, missing, pt.trans)
        }

    def name(pt: PerfType)(implicit lang: Lang) =
      trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, pt.trans)
  }

  case class MaxRating(rating: Int) extends Condition {

    def apply(
        getMaxRating: GetMaxRating
    )(user: User, pt: PerfType)(implicit ec: scala.concurrent.ExecutionContext): Fu[Verdict] =
      if (user.perfs(pt).provisional) fuccess(Refused { implicit lang =>
        trans.yourPerfRatingIsProvisional.txt(pt.trans)
      })
      else if (user.perfs(pt).intRating > rating) fuccess(Refused { implicit lang =>
        trans.yourPerfRatingIsTooHigh.txt(pt.trans, user.perfs(pt).intRating)
      })
      else
        getMaxRating(pt) map {
          case r if r <= rating => Accepted
          case r =>
            Refused { implicit lang =>
              trans.yourTopWeeklyPerfRatingIsTooHigh.txt(pt.trans, r)
            }
        }

    def maybe(user: User, pt: PerfType): Boolean =
      !user.perfs(pt).provisional && user.perfs(pt).intRating <= rating

    def name(pt: PerfType)(implicit lang: Lang) = trans.ratedLessThanInPerf.txt(rating, pt.trans)
  }

  case class MinRating(rating: Int) extends Condition with FlatCond {

    def apply(user: User, pt: PerfType) =
      if (user.hasTitle) Accepted
      else if (user.perfs(pt).provisional) Refused { implicit lang =>
        trans.yourPerfRatingIsProvisional.txt(pt.trans)
      }
      else if (user.perfs(pt).intRating < rating) Refused { implicit lang =>
        trans.yourPerfRatingIsTooLow.txt(pt.trans, user.perfs(pt).intRating)
      }
      else Accepted

    def name(pt: PerfType)(implicit lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, pt.trans)
  }

  case class TeamMember(teamId: TeamID, teamName: TeamName) extends Condition {
    def name(pt: PerfType)(implicit lang: Lang) = trans.mustBeInTeam.txt(teamName)
    def apply(user: User, getUserTeamIds: User.ID => Fu[List[TeamID]])(implicit
        ec: scala.concurrent.ExecutionContext
    ) =
      getUserTeamIds(user.id) map { userTeamIds =>
        if (userTeamIds contains teamId) Accepted
        else
          Refused { implicit lang =>
            trans.youAreNotInTeam.txt(teamName)
          }
      }
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember]
  ) {

    lazy val list: List[Condition] = List(nbRatedGame, maxRating, minRating, titled, teamMember).flatten

    def relevant = list.nonEmpty

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(
        getMaxRating: GetMaxRating,
        getUserTeamIds: User.ID => Fu[List[TeamID]]
    )(user: User, perfType: PerfType)(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user, perfType) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, perfType))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
      }.sequenceFu dmap All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember

    def isRatingLimited = maxRating.isDefined || minRating.isDefined
  }

  object All {
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      titled = none,
      teamMember = none
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal {
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)
    }
  }

  final class Verify(historyApi: lila.history.HistoryApi) {
    def apply(all: All, user: User, perfType: PerfType, getUserTeamIds: User.ID => Fu[List[TeamID]])(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating, getUserTeamIds)(user, perfType)
    }
    def canEnter(user: User, perfType: PerfType, getUserTeamIds: User.ID => Fu[List[TeamID]])(
        tour: Tournament
    )(implicit ec: scala.concurrent.ExecutionContext): Fu[Boolean] =
      apply(tour.conditions, user, perfType, getUserTeamIds).dmap(_.accepted)
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
    implicit private val TeamMemberHandler = Macros.handler[TeamMember]
    implicit val AllBSONHandler            = Macros.handler[All]
  }

  object JSONHandlers {
    import play.api.libs.json._

    def verdictsFor(verdicts: All.WithVerdicts, pt: PerfType, lang: Lang) =
      Json.obj(
        "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
          Json.obj(
            "condition" -> cond.name(pt)(lang),
            "verdict" -> (verd match {
              case Refused(reason) => reason(lang)
              case Accepted        => JsString("ok")
            })
          )
        },
        "accepted" -> verdicts.accepted
      )
  }

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._

    val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, _) => (0, "No restriction")
      case x      => x
    }
    val nbRatedGame = mapping(
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGame.apply)(_.nb.some)

    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "rating" -> numberIn(maxRatings)
    )(MaxRating.apply)(_.rating.some)

    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "rating" -> numberIn(minRatings)
    )(MinRating.apply)(_.rating.some)

    val teamMember = mapping(
      "teamId" -> optional(text)
    )(TeamMemberSetup.apply)(TeamMemberSetup.unapply)
    case class TeamMemberSetup(teamId: Option[TeamID]) {
      def convert(teams: Map[TeamID, TeamName]): Option[TeamMember] =
        teamId flatMap { id =>
          teams.get(id) map { TeamMember(id, _) }
        }
    }
    object TeamMemberSetup {
      def apply(x: TeamMember): TeamMemberSetup = TeamMemberSetup(x.teamId.some)
    }
    val all = mapping(
      "nbRatedGame" -> optional(nbRatedGame),
      "maxRating"   -> optional(maxRating),
      "minRating"   -> optional(minRating),
      "titled"      -> optional(boolean),
      "teamMember"  -> optional(teamMember)
    )(AllSetup.apply)(AllSetup.unapply)
      .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGame],
        maxRating: Option[MaxRating],
        minRating: Option[MinRating],
        titled: Option[Boolean],
        teamMember: Option[TeamMemberSetup]
    ) {

      def validRatings =
        (minRating, maxRating) match {
          case (Some(min), Some(max)) => min.rating < max.rating
          case _                      => true
        }

      def convert(teams: Map[String, String]) =
        All(
          nbRatedGame,
          maxRating,
          minRating,
          ~titled option Titled,
          teamMember.flatMap(_ convert teams)
        )
    }
    object AllSetup {
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = none,
        minRating = none,
        titled = none,
        teamMember = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame,
          maxRating = all.maxRating,
          minRating = all.minRating,
          titled = all.titled has Titled option true,
          teamMember = all.teamMember.map(TeamMemberSetup.apply)
        )
    }
  }
}
