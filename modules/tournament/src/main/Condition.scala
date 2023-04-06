package lila.tournament

import play.api.i18n.Lang

import lila.hub.LeaderTeam
import lila.hub.LightTeam.*
import lila.i18n.I18nKeys as trans
import lila.rating.{ Perf, PerfType }
import lila.user.{ Title, User }
import lila.common.Json.given

sealed trait Condition:

  def name(using lang: Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)

object Condition:

  trait FlatCond:

    def apply(user: User): Condition.Verdict

  type GetMaxRating = PerfType => Fu[IntRating]

  sealed abstract class Verdict(val accepted: Boolean, val reason: Option[Lang => String])
  case object Accepted                        extends Verdict(true, none)
  case class Refused(because: Lang => String) extends Verdict(false, because.some)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond:
    def name(using lang: Lang) = "Only titled players"
    def apply(user: User) =
      if (user.title.exists(_ != Title.LM) && user.noBot) Accepted
      else Refused(l => name(using l))

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond:

    def apply(user: User) =
      if (user.hasTitle) Accepted
      else
        perf match
          case Some(p) if user.perfs(p).nb >= nb => Accepted
          case Some(p) =>
            Refused { lang =>
              val missing = nb - user.perfs(p).nb
              trans.needNbMorePerfGames.pluralTxt(missing, missing, p.trans(using lang))(using lang)
            }
          case None if user.count.rated >= nb => Accepted
          case None =>
            Refused { lang =>
              val missing = nb - user.count.rated
              trans.needNbMoreGames.pluralSameTxt(missing)(using lang)
            }

    def name(using lang: Lang) =
      perf match
        case None    => trans.moreThanNbRatedGames.pluralSameTxt(nb)
        case Some(p) => trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, p.trans)

  abstract trait RatingCondition:
    val perf: PerfType
    val rating: Int

  case class MaxRating(perf: PerfType, rating: Int) extends Condition with RatingCondition:

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

  case class MinRating(perf: PerfType, rating: Int) extends Condition with RatingCondition with FlatCond:

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

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember],
      allowList: Option[AllowList]
  ):

    lazy val list: List[Condition] =
      List(nbRatedGame, maxRating, minRating, titled, teamMember, allowList).flatten

    def relevant = list.nonEmpty

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(
        getMaxRating: GetMaxRating
    )(user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
      }.parallel dmap All.WithVerdicts.apply

    def withRejoinVerdicts(user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ): Fu[All.WithVerdicts] =
      list.map {
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
        case c             => fuccess(WithVerdict(c, Accepted))
      }.parallel dmap All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember

    def isRatingLimited = maxRating.isDefined || minRating.isDefined

  object All:
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      titled = none,
      teamMember = none,
      allowList = none
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal:
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)

  final class Verify(historyApi: lila.history.HistoryApi):
    def apply(all: All, user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ): Fu[All.WithVerdicts] =
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating)(user, getUserTeamIds)
    def rejoin(all: All, user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ): Fu[All.WithVerdicts] =
      all.withRejoinVerdicts(user, getUserTeamIds)
    def canEnter(user: User, getUserTeamIds: User => Fu[List[TeamId]])(
        tour: Tournament
    )(using Executor): Fu[Boolean] =
      apply(tour.conditions, user, getUserTeamIds).dmap(_.accepted)

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }
    import lila.rating.BSONHandlers.perfTypeKeyHandler
    private given BSONDocumentHandler[NbRatedGame] = Macros.handler
    private given BSONDocumentHandler[MaxRating]   = Macros.handler
    private given BSONDocumentHandler[MinRating]   = Macros.handler
    private given BSONHandler[Titled.type] = quickHandler(
      { case _: BSONValue => Titled },
      _ => BSONBoolean(true)
    )
    private given BSONDocumentHandler[TeamMember] = Macros.handler
    private given BSONDocumentHandler[AllowList]  = Macros.handler
    given BSONDocumentHandler[All]                = Macros.handler

  object JSONHandlers:
    import play.api.libs.json.*

    def verdictsFor(verdicts: All.WithVerdicts, lang: Lang) =
      Json.obj(
        "list" -> verdicts.list.map { case WithVerdict(cond, verd) =>
          Json.obj(
            "condition" -> cond.name(using lang),
            "verdict" -> (verd match
              case Refused(reason) => reason(lang)
              case Accepted        => JsString("ok")
            )
          )
        },
        "accepted" -> verdicts.accepted
      )

    given OWrites[Condition.RatingCondition] = OWrites { r =>
      Json.obj(
        "perf"   -> r.perf.key,
        "rating" -> r.rating
      )
    }

    given OWrites[Condition.NbRatedGame] = OWrites { r =>
      Json
        .obj("nb" -> r.nb)
        .add("perf" -> r.perf.map(_.key))
    }

  object DataForm:
    import play.api.data.Forms.*
    import lila.common.Form.{ *, given }
    val perfAuto = "auto" -> "Auto"
    val perfKeys = "auto" :: PerfType.nonPuzzle.map(_.key)
    def perfChoices(using lang: Lang) =
      perfAuto :: PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, _) => (0, "No restriction")
      case x      => x
    }
    val nbRatedGame = mapping(
      "perf" -> optional(of[Perf.Key].verifying(perfKeys.contains)),
      "nb"   -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(unapply)
    case class NbRatedGameSetup(perf: Option[Perf.Key], nb: Int):
      def convert(tourPerf: PerfType): Option[NbRatedGame] =
        nb > 0 option NbRatedGame(
          if (perf has perfAuto._1) tourPerf.some
          else perf.flatMap(PerfType.apply),
          nb
        )
    object NbRatedGameSetup:
      def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)
    case class RatingSetup(perf: Option[Perf.Key], rating: Option[Int]):
      def actualRating = rating.filter(r => r > 600 && r < 3000)
      def convert[A](tourPerf: PerfType)(f: (PerfType, Int) => A): Option[A] =
        actualRating map { r =>
          f(perf.flatMap(PerfType.apply) | tourPerf, r)
        }
    object RatingSetup:
      def apply(v: (Option[PerfType], Option[Int])): RatingSetup = RatingSetup(v._1.map(_.key), v._2)
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
    def teamMember(leaderTeams: List[LeaderTeam]) =
      mapping(
        "teamId" -> optional(of[TeamId].verifying(id => leaderTeams.exists(_.id == id)))
      )(TeamMemberSetup.apply)(_.teamId.some)
    case class TeamMemberSetup(teamId: Option[TeamId]):
      def convert(teams: Map[TeamId, TeamName]): Option[TeamMember] =
        teamId flatMap { id =>
          teams.get(id) map { TeamMember(id, _) }
        }
    object TeamMemberSetup:
      def apply(x: TeamMember): TeamMemberSetup = TeamMemberSetup(x.teamId.some)
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "nbRatedGame" -> optional(nbRatedGame),
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> optional(boolean),
        "teamMember"  -> optional(teamMember(leaderTeams)),
        "allowList"   -> optional(allowList)
      )(AllSetup.apply)(unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGameSetup],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        titled: Option[Boolean],
        teamMember: Option[TeamMemberSetup],
        allowList: Option[String]
    ):

      def validRatings =
        (minRating.actualRating, maxRating.actualRating) match
          case (Some(min), Some(max)) => min < max
          case _                      => true

      def convert(perf: PerfType, teams: Map[TeamId, TeamName]) =
        All(
          nbRatedGame.flatMap(_ convert perf),
          maxRating.convert(perf)(MaxRating.apply),
          minRating.convert(perf)(MinRating.apply),
          ~titled option Titled,
          teamMember.flatMap(_ convert teams),
          allowList = allowList map AllowList.apply
        )
    object AllSetup:
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none),
        titled = none,
        teamMember = none,
        allowList = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame.map(NbRatedGameSetup.apply),
          maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating)),
          titled = all.titled has Titled option true,
          teamMember = all.teamMember.map(TeamMemberSetup.apply),
          allowList = all.allowList.map(_.value)
        )
