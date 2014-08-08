package lila.history

import org.joda.time.{ Days, DateTime }
import lila.rating.PerfType

case class History(
    standard: RatingsMap,
    chess960: RatingsMap,
    kingOfTheHill: RatingsMap,
    threeCheck: RatingsMap,
    bullet: RatingsMap,
    blitz: RatingsMap,
    classical: RatingsMap,
    puzzle: RatingsMap,
    pools: Map[String, RatingsMap]) {

  def apply(perfType: PerfType): RatingsMap = perfType match {
    case PerfType.Standard      => standard
    case PerfType.Bullet        => bullet
    case PerfType.Blitz         => blitz
    case PerfType.Classical     => classical
    case PerfType.Chess960      => chess960
    case PerfType.KingOfTheHill => kingOfTheHill
    case PerfType.ThreeCheck    => threeCheck
    case PerfType.Puzzle        => puzzle
    case PerfType.Pool          => Nil
  }
}

object History {

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.Map.MapReader

  private[history] implicit val BSONReader = new BSONDocumentReader[History] {

    private implicit val ratingsMapReader = new BSONDocumentReader[RatingsMap] {
      def read(doc: BSONDocument): RatingsMap = doc.stream.flatMap {
        case scala.util.Success((k, BSONInteger(v))) => parseIntOption(k) map (_ -> v)
        case _                                       => none[(Int, Int)]
      }.toList sortBy (_._1)
    }

    def read(doc: BSONDocument): History = {
      def ratingsMap(key: String): RatingsMap = ~doc.getAs[RatingsMap](key)
      History(
        standard = ratingsMap("standard"),
        chess960 = ratingsMap("chess960"),
        kingOfTheHill = ratingsMap("kingOfTheHill"),
        threeCheck = ratingsMap("threeCheck"),
        bullet = ratingsMap("bullet"),
        blitz = ratingsMap("blitz"),
        classical = ratingsMap("classical"),
        puzzle = ratingsMap("puzzle"),
        pools = doc.getAs[Map[String, RatingsMap]]("pools") | Map.empty)
    }
  }
}
