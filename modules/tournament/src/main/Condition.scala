package lila.tournament

import lila.perfStat.PerfStat
import lila.rating.PerfType
import lila.user.User

sealed abstract class Condition(val key: String) {

  def apply(stats: Condition.GetStats)(user: User): Fu[Condition.Verdict]

  def name: String

  def withVerdict(stats: Condition.GetStats)(user: User): Fu[Condition.WithVerdict] =
    apply(stats)(user) map { Condition.WithVerdict(this, _) }

  override def toString = name
}

object Condition {

  type GetStats = PerfType => Fu[PerfStat]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted extends Verdict(true)
  case class Refused(reason: String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition("nb") {

    def apply(stats: GetStats)(user: User) = fuccess {
      perf match {
        case Some(p) if user.perfs(p).nb >= nb => Accepted
        case Some(p)                           => Refused(s"Only ${user.perfs(p).nb} of $nb rated ${p.name} games played")
        case None if user.count.rated >= nb    => Accepted
        case None                              => Refused(s"Only ${user.count.rated} of $nb rated games played")
      }
    }

    def name = perf match {
      case None    => s"≥ $nb rated games"
      case Some(p) => s"≥ $nb ${p.name} rated games"
    }
  }

  case class MaxRating(perf: PerfType, rating: Int) extends Condition("rating") {

    def apply(stats: GetStats)(user: User) = stats(perf) map { s =>
      s.highest match {
        case None                       => Accepted
        case Some(h) if h.int <= rating => Accepted
        case Some(h)                    => Refused(s"Max ${perf.name} rating (${h.int}) is too high.")
      }
    }

    def name = s"Rated ≤ $rating in ${perf.name}"
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating]) {

    def relevant = list.nonEmpty

    def list: List[Condition] = List(nbRatedGame, maxRating).flatten

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(stats: Condition.GetStats)(user: User): Fu[All.WithVerdicts] =
      list.map { cond =>
        cond.withVerdict(stats)(user)
      }.sequenceFu map All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })
  }

  object All {
    val empty = All(nbRatedGame = none, maxRating = none)

    case class WithVerdicts(list: List[WithVerdict]) {
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)
    }
  }

  final class Verify(getStats: PerfStat.Getter) {
    def apply(all: All, user: User): Fu[All.WithVerdicts] = {
      val stats: GetStats = perf => getStats(user, perf)
      all.withVerdicts(stats)(user)
    }
  }

  object BSONHandlers {
    import reactivemongo.bson._
    import lila.db.BSON
    import lila.db.BSON.{ Reader, Writer }
    import lila.db.dsl._
    private implicit val PerfTypeBSONHandler = new BSONHandler[BSONString, PerfType] {
      def read(bs: BSONString): PerfType = PerfType(bs.value) err s"No such PerfType: ${bs.value}"
      def write(x: PerfType) = BSONString(x.key)
    }
    private implicit val NbRatedGameHandler = Macros.handler[NbRatedGame]
    private implicit val MaxRatingHandler = Macros.handler[MaxRating]
    implicit val AllBSONHandler = Macros.handler[All]
  }

  object JSONHandlers {
    import play.api.libs.json._
    private implicit val perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
      Json.obj("key" -> pt.key, "name" -> pt.name)
    }
    implicit val NbRatedGameJSONWriter = Json.writes[NbRatedGame]
    implicit val MaxRatingJSONWriter = Json.writes[MaxRating]
    private implicit val conditionWriter: OWrites[Condition] = OWrites {
      case x: NbRatedGame => NbRatedGameJSONWriter writes x
      case x: MaxRating   => MaxRatingJSONWriter writes x
    }
    implicit val AllJSONWriter = Json.writes[All]
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
    val all = mapping(
      "nbRatedGame" -> nbRatedGame,
      "maxRating" -> maxRating
    )(AllSetup.apply)(AllSetup.unapply)
    case class AllSetup(nbRatedGame: NbRatedGameSetup, maxRating: MaxRatingSetup) {
      def convert = All(nbRatedGame.convert, maxRating.convert)
    }
    object AllSetup {
      val default = AllSetup(nbRatedGame = NbRatedGameSetup.default, maxRating = MaxRatingSetup.default)
      def apply(all: All): AllSetup = AllSetup(
        nbRatedGame = all.nbRatedGame.fold(NbRatedGameSetup.default)(NbRatedGameSetup.apply),
        maxRating = all.maxRating.fold(MaxRatingSetup.default)(MaxRatingSetup.apply))
    }
  }
}
