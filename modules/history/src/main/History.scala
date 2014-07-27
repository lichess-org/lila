package lila.history

import org.joda.time.{ Days, DateTime }

case class History(
  standard: RatingsMap,
  chess960: RatingsMap,
  kingOfTheHill: RatingsMap,
  bullet: RatingsMap,
  blitz: RatingsMap,
  classical: RatingsMap,
  puzzle: RatingsMap,
  pools: Map[String, RatingsMap])

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
        bullet = ratingsMap("bullet"),
        blitz = ratingsMap("blitz"),
        classical = ratingsMap("classical"),
        puzzle = ratingsMap("puzzle"),
        pools = doc.getAs[Map[String, RatingsMap]]("pools") | Map.empty)
    }
  }
}
