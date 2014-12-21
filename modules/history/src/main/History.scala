package lila.history

import lila.rating.PerfType
import org.joda.time.{ Days, DateTime }

case class History(
    standard: RatingsMap,
    chess960: RatingsMap,
    kingOfTheHill: RatingsMap,
    suicide: RatingsMap,
    threeCheck: RatingsMap,
    bullet: RatingsMap,
    blitz: RatingsMap,
    classical: RatingsMap,
    correspondence: RatingsMap,
    puzzle: RatingsMap) {

  def apply(perfType: PerfType): RatingsMap = perfType match {
    case PerfType.Standard       => standard
    case PerfType.Bullet         => bullet
    case PerfType.Blitz          => blitz
    case PerfType.Classical      => classical
    case PerfType.Correspondence => correspondence
    case PerfType.Chess960       => chess960
    case PerfType.KingOfTheHill  => kingOfTheHill
    case PerfType.Suicide        => suicide
    case PerfType.ThreeCheck     => threeCheck
    case PerfType.Puzzle         => puzzle
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
        suicide = ratingsMap("suicide"),
        bullet = ratingsMap("bullet"),
        blitz = ratingsMap("blitz"),
        classical = ratingsMap("classical"),
        correspondence = ratingsMap("correspondence"),
        puzzle = ratingsMap("puzzle"))
    }
  }
}
