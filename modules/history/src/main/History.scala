package lila.history

import lila.rating.PerfType

case class History(
    standard: RatingsMap,
    chess960: RatingsMap,
    kingOfTheHill: RatingsMap,
    antichess: RatingsMap,
    threeCheck: RatingsMap,
    atomic: RatingsMap,
    horde: RatingsMap,
    racingKings: RatingsMap,
    crazyhouse: RatingsMap,
    ultraBullet: RatingsMap,
    bullet: RatingsMap,
    blitz: RatingsMap,
    rapid: RatingsMap,
    classical: RatingsMap,
    correspondence: RatingsMap,
    puzzle: RatingsMap
) {

  def apply(perfType: PerfType): RatingsMap = perfType match {
    case PerfType.Standard => standard
    case PerfType.Bullet => bullet
    case PerfType.Blitz => blitz
    case PerfType.Rapid => rapid
    case PerfType.Classical => classical
    case PerfType.Correspondence => correspondence
    case PerfType.Chess960 => chess960
    case PerfType.KingOfTheHill => kingOfTheHill
    case PerfType.Antichess => antichess
    case PerfType.ThreeCheck => threeCheck
    case PerfType.Atomic => atomic
    case PerfType.Horde => horde
    case PerfType.RacingKings => racingKings
    case PerfType.Crazyhouse => crazyhouse
    case PerfType.Puzzle => puzzle
    case PerfType.UltraBullet => ultraBullet
    case x => sys error s"No history for perf $x"
  }
}

object History {

  import reactivemongo.bson._

  private[history] implicit val RatingsMapReader = new BSONDocumentReader[RatingsMap] {
    def read(doc: BSONDocument): RatingsMap = doc.stream.flatMap {
      case scala.util.Success(BSONElement(k, BSONInteger(v))) => parseIntOption(k) map (_ -> v)
      case _ => none[(Int, Int)]
    }.toList sortBy (_._1)
  }

  private[history] implicit val HistoryBSONReader = new BSONDocumentReader[History] {

    def read(doc: BSONDocument): History = {
      def ratingsMap(key: String): RatingsMap = ~doc.getAs[RatingsMap](key)
      History(
        standard = ratingsMap("standard"),
        chess960 = ratingsMap("chess960"),
        kingOfTheHill = ratingsMap("kingOfTheHill"),
        threeCheck = ratingsMap("threeCheck"),
        antichess = ratingsMap("antichess"),
        atomic = ratingsMap("atomic"),
        horde = ratingsMap("horde"),
        racingKings = ratingsMap("racingKings"),
        crazyhouse = ratingsMap("crazyhouse"),
        ultraBullet = ratingsMap("ultraBullet"),
        bullet = ratingsMap("bullet"),
        blitz = ratingsMap("blitz"),
        rapid = ratingsMap("rapid"),
        classical = ratingsMap("classical"),
        correspondence = ratingsMap("correspondence"),
        puzzle = ratingsMap("puzzle")
      )
    }
  }
}
