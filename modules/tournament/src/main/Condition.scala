package lila.tournament

import lila.common.Lang
import lila.hub.lightTeam._
import lila.i18n.I18nKeys
import lila.rating.BSONHandlers.perfTypeKeyHandler
import lila.rating.PerfType
import lila.user.{ User, Title }

sealed trait Condition {

  def name(lang: Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)
}

object Condition {

  trait FlatCond {

    def apply(user: User): Condition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted extends Verdict(true)
  case class Refused(reason: Lang => String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond {
    def name(lang: Lang) = "Only titled players"
    def apply(user: User) =
      if (user.title.exists(_ != Title.LM)) Accepted
      else Refused(name _)
  }

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond {

    def apply(user: User) =
      if (user.hasTitle) Accepted
      else perf match {
        case Some(p) if user.perfs(p).nb >= nb => Accepted
        case Some(p) => Refused { lang =>
          val missing = nb - user.perfs(p).nb
          I18nKeys.needNbMorePerfGames.pluralTxtTo(lang, missing, List(missing, p.name))
        }
        case None if user.count.rated >= nb => Accepted
        case None => Refused { lang =>
          val missing = nb - user.count.rated
          I18nKeys.needNbMoreGames.pluralTxtTo(lang, missing, List(missing))
        }
      }

    def name(lang: Lang) = perf match {
      case None => I18nKeys.moreThanNbRatedGames.pluralTxtTo(lang, nb, List(nb))
      case Some(p) => I18nKeys.moreThanNbPerfRatedGames.pluralTxtTo(lang, nb, List(nb, p.name))
    }
  }

  case class MaxRating(perf: PerfType, rating: Int) extends Condition {

    def apply(getMaxRating: GetMaxRating)(user: User): Fu[Verdict] =
      if (user.perfs(perf).provisional) fuccess(Refused { lang =>
        I18nKeys.yourPerfRatingIsProvisional.literalTxtTo(lang, perf.name)
      })
      else if (user.perfs(perf).intRating > rating) fuccess(Refused { lang =>
        I18nKeys.yourPerfRatingIsTooHigh.literalTxtTo(lang, List(perf.name, user.perfs(perf).intRating))
      })
      else getMaxRating(perf) map {
        case r if r <= rating => Accepted
        case r => Refused { lang =>
          I18nKeys.yourTopWeeklyPerfRatingIsTooHigh.literalTxtTo(lang, List(perf.name, r))
        }
      }

    def maybe(user: User): Boolean =
      !user.perfs(perf).provisional && user.perfs(perf).intRating <= rating

    def name(lang: Lang) = I18nKeys.ratedLessThanInPerf.literalTxtTo(lang, List(rating, perf.name))
  }

  case class MinRating(perf: PerfType, rating: Int) extends Condition with FlatCond {

    def apply(user: User) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).provisional) Refused { lang =>
        I18nKeys.yourPerfRatingIsProvisional.literalTxtTo(lang, perf.name)
      }
      else if (user.perfs(perf).intRating < rating) Refused { lang =>
        I18nKeys.yourPerfRatingIsTooLow.literalTxtTo(lang, List(perf.name, user.perfs(perf).intRating))
      }
      else Accepted

    def name(lang: Lang) = I18nKeys.ratedMoreThanInPerf.literalTxtTo(lang, List(rating, perf.name))
  }

  case class TeamMember(teamId: TeamId, teamName: TeamName) extends Condition {
    def name(lang: Lang) = I18nKeys.mustBeInTeam.literalTxtTo(lang, List(teamName))
    def apply(user: User, getUserTeamIds: User => Fu[TeamIdList]) =
      getUserTeamIds(user) map { userTeamIds =>
        if (userTeamIds contains teamId) Accepted
        else Refused { lang => I18nKeys.youAreNotInTeam.literalTxtTo(lang, List(teamName)) }
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

    def withVerdicts(getMaxRating: GetMaxRating)(user: User, getUserTeamIds: User => Fu[TeamIdList]): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond => fuccess(c withVerdict c(user))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
      }.sequenceFu map All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All) = sameMaxRating(other) && sameMinRating(other)

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
    def apply(all: All, user: User, getUserTeamIds: User => Fu[TeamIdList]): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating)(user, getUserTeamIds)
    }
    def canEnter(user: User, getUserTeamIds: User => Fu[TeamIdList])(tour: Tournament): Fu[Boolean] =
      apply(tour.conditions, user, getUserTeamIds).map(_.accepted)
  }

  object BSONHandlers {
    import reactivemongo.bson._
    private implicit val NbRatedGameHandler = Macros.handler[NbRatedGame]
    private implicit val MaxRatingHandler = Macros.handler[MaxRating]
    private implicit val MinRatingHandler = Macros.handler[MinRating]
    private implicit val TitledHandler = new BSONHandler[BSONValue, Titled.type] {
      def read(x: BSONValue) = Titled
      def write(x: Titled.type) = BSONBoolean(true)
    }
    private implicit val TeamMemberHandler = Macros.handler[TeamMember]
    implicit val AllBSONHandler = Macros.handler[All]
  }

  object JSONHandlers {
    import play.api.libs.json._
    private implicit val perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
      Json.obj("key" -> pt.key, "name" -> pt.name)
    }

    def verdictsFor(verdicts: All.WithVerdicts, lang: Lang) = Json.obj(
      "list" -> verdicts.list.map {
        case WithVerdict(cond, verd) => Json.obj(
          "condition" -> (cond name lang),
          "verdict" -> (verd match {
            case Refused(reason) => reason(lang)
            case Accepted => JsString("ok")
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
    val perfChoices = perfAuto :: PerfType.nonPuzzle.map { pt =>
      pt.key -> pt.name
    }
    val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, name) => (0, "No restriction")
      case x => x
    }
    val nbRatedGame = mapping(
      "perf" -> optional(text.verifying(perfChoices.toMap.contains _)),
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(NbRatedGameSetup.unapply)
    case class NbRatedGameSetup(perf: Option[String], nb: Int) {
      def isDefined = nb > 0
      def convert(tourPerf: PerfType) = isDefined option NbRatedGame(
        if (perf has perfAuto._1) tourPerf.some else PerfType(~perf),
        nb
      )
    }
    object NbRatedGameSetup {
      val default = NbRatedGameSetup(perfAuto._1.some, 0)
      def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)
    }
    val maxRatings = List(9999, 2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1000)
    val maxRatingChoices = options(maxRatings, "Max rating of %d") map {
      case (9999, name) => (9999, "No restriction")
      case x => x
    }
    val maxRating = mapping(
      "perf" -> text.verifying(perfChoices.toMap.contains _),
      "rating" -> numberIn(maxRatingChoices)
    )(MaxRatingSetup.apply)(MaxRatingSetup.unapply)
    case class MaxRatingSetup(perf: String, rating: Int) {
      def isDefined = rating < 9000
      def convert(tourPerf: PerfType) = isDefined option MaxRating(PerfType(perf) | tourPerf, rating)
    }
    object MaxRatingSetup {
      val default = MaxRatingSetup(perfAuto._1, 9999)
      def apply(x: MaxRating): MaxRatingSetup = MaxRatingSetup(x.perf.key, x.rating)
    }
    val minRatings = List(0, 1600, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)
    val minRatingChoices = options(minRatings, "Min rating of %d") map {
      case (0, name) => (0, "No restriction")
      case x => x
    }
    val minRating = mapping(
      "perf" -> text.verifying(perfChoices.toMap.contains _),
      "rating" -> numberIn(minRatingChoices)
    )(MinRatingSetup.apply)(MinRatingSetup.unapply)
    case class MinRatingSetup(perf: String, rating: Int) {
      def isDefined = rating > 0
      def convert(tourPerf: PerfType) = isDefined option MinRating(PerfType(perf) | tourPerf, rating)
    }
    object MinRatingSetup {
      val default = MinRatingSetup(perfAuto._1, 0)
      def apply(x: MinRating): MinRatingSetup = MinRatingSetup(x.perf.key, x.rating)
    }
    val teamMember = mapping(
      "teamId" -> optional(text)
    )(TeamMemberSetup.apply)(TeamMemberSetup.unapply)
    case class TeamMemberSetup(teamId: Option[TeamId]) {
      def convert(teams: Map[TeamId, TeamName]): Option[TeamMember] =
        teamId flatMap { id =>
          teams.get(id) map { TeamMember(id, _) }
        }
    }
    object TeamMemberSetup {
      val default = TeamMemberSetup(None)
      def apply(x: TeamMember): TeamMemberSetup = TeamMemberSetup(x.teamId.some)
    }
    val all = mapping(
      "nbRatedGame" -> nbRatedGame,
      "maxRating" -> maxRating,
      "minRating" -> minRating,
      "titled" -> boolean,
      "teamMember" -> teamMember
    )(AllSetup.apply)(AllSetup.unapply)
      .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: NbRatedGameSetup,
        maxRating: MaxRatingSetup,
        minRating: MinRatingSetup,
        titled: Boolean,
        teamMember: TeamMemberSetup
    ) {

      def validRatings = !maxRating.isDefined || !minRating.isDefined || {
        maxRating.rating > minRating.rating
      }

      def convert(perf: PerfType, teams: Map[String, String]) = All(
        nbRatedGame convert perf,
        maxRating convert perf,
        minRating convert perf,
        titled option Titled,
        teamMember convert teams
      )
    }
    object AllSetup {
      val default = AllSetup(
        nbRatedGame = NbRatedGameSetup.default,
        maxRating = MaxRatingSetup.default,
        minRating = MinRatingSetup.default,
        titled = false,
        teamMember = TeamMemberSetup.default
      )
      def apply(all: All): AllSetup = AllSetup(
        nbRatedGame = all.nbRatedGame.fold(NbRatedGameSetup.default)(NbRatedGameSetup.apply),
        maxRating = all.maxRating.fold(MaxRatingSetup.default)(MaxRatingSetup.apply),
        minRating = all.minRating.fold(MinRatingSetup.default)(MinRatingSetup.apply),
        titled = all.titled.isDefined,
        teamMember = all.teamMember.fold(TeamMemberSetup.default)(TeamMemberSetup.apply)
      )
    }
  }
}
