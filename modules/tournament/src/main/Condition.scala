package lila.tournament

import lila.rating.PerfType
import lila.user.User

sealed trait Condition {

  def name: String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)

  override def toString = name
}

object Condition {

  trait FlatCond {

    def apply(user: User): Condition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted extends Verdict(true)
  case class Refused(reason: String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond {

    def apply(user: User) =
      perf match {
        case Some(p) if user.perfs(p).nb >= nb => Accepted
        case Some(p)                           => Refused(s"Only ${user.perfs(p).nb} of $nb rated ${p.name} games played")
        case None if user.count.rated >= nb    => Accepted
        case None                              => Refused(s"Only ${user.count.rated} of $nb rated games played")
      }

    def name = perf match {
      case None    => s"≥ $nb rated games"
      case Some(p) => s"≥ $nb ${p.name} rated games"
    }
  }

  case class MaxRating(perf: PerfType, rating: Int) extends Condition {

    def apply(getMaxRating: GetMaxRating)(user: User) =
      if (user.perfs(perf).provisional) fuccess(Refused(s"Provisional ${perf.name} rating"))
      else getMaxRating(perf) map {
        case r if r <= rating => Accepted
        case r                => Refused(s"Top monthly ${perf.name} rating ($r) is too high")
      }

    def name = s"Rated ≤ $rating in ${perf.name}"
  }

  case class MinRating(perf: PerfType, rating: Int) extends Condition with FlatCond {

    def apply(user: User) =
      if (user.perfs(perf).provisional) Refused(s"Provisional ${perf.name} rating")
      else if (user.perfs(perf).intRating < rating) Refused(s"Current ${perf.name} rating is too low")
      else Accepted

    def name = s"Rated ≥ $rating in ${perf.name}"
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating]) {

    def relevant = list.nonEmpty

    def list: List[Condition] = List(nbRatedGame, maxRating, minRating).flatten

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(getMaxRating: GetMaxRating)(user: User): Fu[All.WithVerdicts] =
      list.map {
        case c: MaxRating => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond  => fuccess(c withVerdict c(user))
      }.sequenceFu map All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
  }

  object All {
    val empty = All(nbRatedGame = none, maxRating = none, minRating = none)

    case class WithVerdicts(list: List[WithVerdict]) {
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)
    }
  }

  final class Verify(historyApi: lila.history.HistoryApi) {
    def apply(all: All, user: User): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating)(user)
    }
    def canEnter(user: User)(tour: Tournament): Fu[Boolean] =
      apply(tour.conditions, user).map(_.accepted)
  }

  object BSONHandlers {
    import reactivemongo.bson._
    import lila.db.BSON
    import lila.db.dsl._
    private implicit val PerfTypeBSONHandler = new BSONHandler[BSONString, PerfType] {
      def read(bs: BSONString): PerfType = PerfType(bs.value) err s"No such PerfType: ${bs.value}"
      def write(x: PerfType) = BSONString(x.key)
    }
    private implicit val NbRatedGameHandler = Macros.handler[NbRatedGame]
    private implicit val MaxRatingHandler = Macros.handler[MaxRating]
    private implicit val MinRatingHandler = Macros.handler[MinRating]
    implicit val AllBSONHandler = Macros.handler[All]
  }

  object JSONHandlers {
    import play.api.libs.json._
    private implicit val perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
      Json.obj("key" -> pt.key, "name" -> pt.name)
    }
    private implicit val ConditionWriter: Writes[Condition] = Writes { o =>
      JsString(o.name)
    }
    private implicit val VerdictWriter: Writes[Verdict] = Writes {
      case Refused(reason) => JsString(reason)
      case Accepted        => JsString("ok")
    }
    implicit val AllJSONWriter = Json.writes[All]
    implicit val WithVerdictJSONWriter = Json.writes[WithVerdict]
    implicit val AllWithVerdictsJSONWriter = Writes[All.WithVerdicts] { o =>
      Json.obj("list" -> o.list, "accepted" -> o.accepted)
    }
  }

  object DataForm {
    import play.api.data._
    import play.api.data.Forms._
    import lila.common.Form._
    val perfChoices = PerfType.nonPuzzle.map { pt =>
      pt.key -> pt.name
    }
    val nbRatedGames = Seq(0, 5, 10, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, name) => (0, "No restriction")
      case x         => x
    }
    val nbRatedGame = mapping(
      "perf" -> optional(text.verifying(perfChoices.toMap.contains _)),
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(NbRatedGameSetup.unapply)
    case class NbRatedGameSetup(perf: Option[String], nb: Int) {
      def isDefined = nb > 0
      def convert = isDefined option NbRatedGame(PerfType(~perf), nb)
    }
    object NbRatedGameSetup {
      val default = NbRatedGameSetup(PerfType.Blitz.key.some, 0)
      def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)
    }
    val maxRatings = List(9999, 2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1000)
    val maxRatingChoices = options(maxRatings, "Max rating of %d") map {
      case (9999, name) => (9999, "No restriction")
      case x            => x
    }
    val maxRating = mapping(
      "perf" -> text.verifying(perfChoices.toMap.contains _),
      "rating" -> numberIn(maxRatingChoices)
    )(MaxRatingSetup.apply)(MaxRatingSetup.unapply)
    case class MaxRatingSetup(perf: String, rating: Int) {
      def isDefined = rating < 9000
      def convert = isDefined option MaxRating(PerfType(perf) err s"perf $perf", rating)
    }
    object MaxRatingSetup {
      val default = MaxRatingSetup(PerfType.Blitz.key, 9999)
      def apply(x: MaxRating): MaxRatingSetup = MaxRatingSetup(x.perf.key, x.rating)
    }
    val minRatings = List(0, 1600, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)
    val minRatingChoices = options(minRatings, "Min rating of %d") map {
      case (0, name) => (0, "No restriction")
      case x         => x
    }
    val minRating = mapping(
      "perf" -> text.verifying(perfChoices.toMap.contains _),
      "rating" -> numberIn(minRatingChoices)
    )(MinRatingSetup.apply)(MinRatingSetup.unapply)
    case class MinRatingSetup(perf: String, rating: Int) {
      def isDefined = rating > 0
      def convert = isDefined option MinRating(PerfType(perf) err s"perf $perf", rating)
    }
    object MinRatingSetup {
      val default = MinRatingSetup(PerfType.Blitz.key, 0)
      def apply(x: MinRating): MinRatingSetup = MinRatingSetup(x.perf.key, x.rating)
    }
    val all = mapping(
      "nbRatedGame" -> nbRatedGame,
      "maxRating" -> maxRating,
      "minRating" -> minRating
    )(AllSetup.apply)(AllSetup.unapply)

    case class AllSetup(
        nbRatedGame: NbRatedGameSetup,
        maxRating: MaxRatingSetup,
        minRating: MinRatingSetup) {
      def convert = All(nbRatedGame.convert, maxRating.convert, minRating.convert)
    }
    object AllSetup {
      val default = AllSetup(
        nbRatedGame = NbRatedGameSetup.default,
        maxRating = MaxRatingSetup.default,
        minRating = MinRatingSetup.default)
      def apply(all: All): AllSetup = AllSetup(
        nbRatedGame = all.nbRatedGame.fold(NbRatedGameSetup.default)(NbRatedGameSetup.apply),
        maxRating = all.maxRating.fold(MaxRatingSetup.default)(MaxRatingSetup.apply),
        minRating = all.minRating.fold(MinRatingSetup.default)(MinRatingSetup.apply))
    }
  }
}
