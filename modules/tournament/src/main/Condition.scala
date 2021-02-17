package lila.tournament

import play.api.i18n.Lang

import lila.hub.LeaderTeam
import lila.hub.LightTeam._
import lila.i18n.{ I18nKeys => trans }
import lila.rating.BSONHandlers.perfTypeKeyHandler
import lila.rating.PerfType
import lila.user.{ Title, User }

sealed trait Condition {

  def name(implicit lang: Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)
}

object Condition {

  trait FlatCond {

    def apply(user: User): Condition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted                       extends Verdict(true)
  case class Refused(reason: Lang => String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond {
    def name(implicit lang: Lang) = "Only titled players"
    def apply(user: User) =
      if (user.title.exists(_ != Title.LM) && user.noBot) Accepted
      else Refused(name(_))
  }

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond {

    def apply(user: User) =
      if (user.hasTitle) Accepted
      else
        perf match {
          case Some(p) if user.perfs(p).nb >= nb => Accepted
          case Some(p) =>
            Refused { implicit lang =>
              val missing = nb - user.perfs(p).nb
              trans.needNbMorePerfGames.pluralTxt(missing, missing, p.trans)
            }
          case None if user.count.rated >= nb => Accepted
          case None =>
            Refused { implicit lang =>
              val missing = nb - user.count.rated
              trans.needNbMoreGames.pluralSameTxt(missing)
            }
        }

    def name(implicit lang: Lang) =
      perf match {
        case None    => trans.moreThanNbRatedGames.pluralSameTxt(nb)
        case Some(p) => trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, p.trans)
      }
  }

  case class MaxRating(perf: PerfType, rating: Int) extends Condition {

    def apply(
        getMaxRating: GetMaxRating
    )(user: User)(implicit ec: scala.concurrent.ExecutionContext): Fu[Verdict] =
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

    def maybe(user: User): Boolean =
      !user.perfs(perf).provisional && user.perfs(perf).intRating <= rating

    def name(implicit lang: Lang) = trans.ratedLessThanInPerf.txt(rating, perf.trans)
  }

  case class MinRating(perf: PerfType, rating: Int) extends Condition with FlatCond {

    def apply(user: User) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).provisional) Refused { implicit lang =>
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      }
      else if (user.perfs(perf).intRating < rating) Refused { implicit lang =>
        trans.yourPerfRatingIsTooLow.txt(perf.trans, user.perfs(perf).intRating)
      }
      else Accepted

    def name(implicit lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, perf.trans)
  }

  case class TeamMember(teamId: TeamID, teamName: TeamName) extends Condition {
    def name(implicit lang: Lang) = trans.mustBeInTeam.txt(teamName)
    def apply(user: User, getUserTeamIds: User => Fu[List[TeamID]])(implicit
        ec: scala.concurrent.ExecutionContext
    ) =
      getUserTeamIds(user) map { userTeamIds =>
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
        getMaxRating: GetMaxRating
    )(user: User, getUserTeamIds: User => Fu[List[TeamID]])(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user))
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
    def apply(all: All, user: User, getUserTeamIds: User => Fu[List[TeamID]])(implicit
        ec: scala.concurrent.ExecutionContext
    ): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating)(user, getUserTeamIds)
    }
    def canEnter(user: User, getUserTeamIds: User => Fu[List[TeamID]])(
        tour: Tournament
    )(implicit ec: scala.concurrent.ExecutionContext): Fu[Boolean] =
      apply(tour.conditions, user, getUserTeamIds).dmap(_.accepted)
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

    def verdictsFor(verdicts: All.WithVerdicts, lang: Lang) =
      Json.obj(
        "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
          Json.obj(
            "condition" -> (cond name lang),
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
    val perfAuto = "auto" -> "Auto"
    val perfKeys = "auto" :: PerfType.nonPuzzle.map(_.key)
    def perfChoices(implicit lang: Lang) =
      perfAuto :: PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, _) => (0, "No restriction")
      case x      => x
    }
    val nbRatedGame = mapping(
      "perf" -> optional(text.verifying(perfKeys.contains _)),
      "nb"   -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(NbRatedGameSetup.unapply)
    case class NbRatedGameSetup(perf: Option[String], nb: Int) {
      def convert(tourPerf: PerfType): Option[NbRatedGame] =
        nb > 0 option NbRatedGame(
          if (perf has perfAuto._1) tourPerf.some else PerfType(~perf),
          nb
        )
    }
    object NbRatedGameSetup {
      def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)
    }
    case class RatingSetup(perf: Option[String], rating: Option[Int]) {
      def actualRating = rating.filter(r => r > 600 && r < 3000)
      def convert[A](tourPerf: PerfType)(f: (PerfType, Int) => A): Option[A] =
        actualRating map { r =>
          f(perf.flatMap(PerfType.apply) | tourPerf, r)
        }
    }
    object RatingSetup {
      def apply(v: (Option[PerfType], Option[Int])): RatingSetup = RatingSetup(v._1.map(_.key), v._2)
    }
    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "perf"   -> optional(text.verifying(perfKeys.contains _)),
      "rating" -> optional(numberIn(maxRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)
    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "perf"   -> optional(text.verifying(perfKeys.contains _)),
      "rating" -> optional(numberIn(minRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)
    def teamMember(leaderTeams: List[LeaderTeam]) =
      mapping(
        "teamId" -> optional(text.verifying(id => leaderTeams.exists(_.id == id)))
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
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "nbRatedGame" -> optional(nbRatedGame),
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> optional(boolean),
        "teamMember"  -> optional(teamMember(leaderTeams))
      )(AllSetup.apply)(AllSetup.unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGameSetup],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        titled: Option[Boolean],
        teamMember: Option[TeamMemberSetup]
    ) {

      def validRatings =
        (minRating.actualRating, maxRating.actualRating) match {
          case (Some(min), Some(max)) => min < max
          case _                      => true
        }

      def convert(perf: PerfType, teams: Map[String, String]) =
        All(
          nbRatedGame.flatMap(_ convert perf),
          maxRating.convert(perf)(MaxRating.apply),
          minRating.convert(perf)(MinRating.apply),
          ~titled option Titled,
          teamMember.flatMap(_ convert teams)
        )
    }
    object AllSetup {
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none),
        titled = none,
        teamMember = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame.map(NbRatedGameSetup.apply),
          maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating)),
          titled = all.titled has Titled option true,
          teamMember = all.teamMember.map(TeamMemberSetup.apply)
        )
    }
  }
}
