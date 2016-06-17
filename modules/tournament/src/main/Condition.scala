package lila.tournament

import lila.perfStat.PerfStat
import lila.rating.PerfType
import lila.user.User

sealed abstract class Condition(val key: String) {

  def apply(stats: Condition.GetStats)(user: User): Fu[Condition.Verdict]
}

object Condition {

  type GetStats = PerfType => Fu[PerfStat]

  sealed trait Verdict
  case object Accepted extends Verdict
  case class Refused(reason: String) extends Verdict

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition("nb") {

    def apply(stats: GetStats)(user: User) = fuccess {
      perf match {
        case Some(p) if user.perfs(p).nb >= nb => Accepted
        case Some(p)                           => Refused(s"Only ${user.perfs(p).nb} of $nb rated ${p.name} games played")
        case None if user.count.rated >= nb    => Accepted
        case None                              => Refused(s"Only ${user.count.rated} of $nb rated games played")
      }
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
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating]) {

    def nonEmpty = nbRatedGame.isDefined || maxRating.isDefined

    def ifNonEmpty = nonEmpty option this
  }

  object All {
    val empty = All(nbRatedGame = none, maxRating = none)
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
    // private implicit val ConditionBSONHandler = new BSON[Condition] {
    //   def reads(r: Reader) = r.str("key") match {
    //     case "nb"     => NbRatedGameHandler read r.doc
    //     case "rating" => MaxRatingHandler read r.doc
    //     case x        => sys error s"Invalid condition key $x"
    //   }
    //   def writes(w: Writer, x: Condition) = x match {
    //     case x: NbRatedGame => NbRatedGameHandler write x
    //     case x: MaxRating   => MaxRatingHandler write x
    //   }
    // }
    implicit val AllBSONHandler = Macros.handler[All]
  }

  object DataForm {
    import play.api.data._
    import play.api.data.Forms._
    import lila.common.Form._
    val nbRatedGames = Seq(5, 10, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}")
    val nbRatedGame = mapping(
      "enforce" -> optional(number),
      "perf" -> optional(text.verifying(PerfType.byKey.contains _)),
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(NbRatedGameSetup.unapply)
    case class NbRatedGameSetup(enforce: Option[Int], perf: Option[String], nb: Int) {
      def convert = enforce.isDefined option NbRatedGame(PerfType(~perf), nb)
    }
    val maxRatings = Seq(1000, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200)
    val maxRatingChoices = options(maxRatings, "max rating of %d")
    val maxRating = mapping(
      "enforce" -> optional(number),
      "perf" -> text.verifying(PerfType.byKey.contains _),
      "rating" -> numberIn(maxRatingChoices)
    )(MaxRatingSetup.apply)(MaxRatingSetup.unapply)
    case class MaxRatingSetup(enforce: Option[Int], perf: String, rating: Int) {
      def convert = enforce.isDefined option MaxRating(PerfType(perf) err perf, rating)
    }
    val all = Form(mapping(
      "nbRatedGame" -> nbRatedGame,
      "maxRating" -> maxRating
    )(AllSetup.apply)(AllSetup.unapply))
    case class AllSetup(nbRatedGame: NbRatedGameSetup, maxRating: MaxRatingSetup) {
      def convert = All(nbRatedGame.convert, maxRating.convert)
    }
  }
}
