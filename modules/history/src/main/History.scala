package lidraughts.history

import draughts.variant.{ Variant, Standard, Frisian, Russian }
import lidraughts.rating.PerfType

case class History(
    standard: RatingsMap,
    frisian: RatingsMap,
    frysk: RatingsMap,
    antidraughts: RatingsMap,
    breakthrough: RatingsMap,
    russian: RatingsMap,
    ultraBullet: RatingsMap,
    bullet: RatingsMap,
    blitz: RatingsMap,
    rapid: RatingsMap,
    classical: RatingsMap,
    correspondence: RatingsMap,
    puzzle: Map[Variant, RatingsMap]
) {

  def apply(perfType: PerfType): RatingsMap = perfType match {
    case PerfType.Standard => standard
    case PerfType.Bullet => bullet
    case PerfType.Blitz => blitz
    case PerfType.Rapid => rapid
    case PerfType.Classical => classical
    case PerfType.Correspondence => correspondence
    case PerfType.Frisian => frisian
    case PerfType.Frysk => frysk
    case PerfType.Antidraughts => antidraughts
    case PerfType.Breakthrough => breakthrough
    case PerfType.Russian => russian
    case PerfType.Puzzle => puzzle(Standard)
    case PerfType.PuzzleFrisian => puzzle(Frisian)
    case PerfType.PuzzleRussian => puzzle(Russian)
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
        frisian = ratingsMap("frisian"),
        frysk = ratingsMap("frysk"),
        antidraughts = ratingsMap("antidraughts"),
        breakthrough = ratingsMap("breakthrough"),
        russian = ratingsMap("russian"),
        ultraBullet = ratingsMap("ultraBullet"),
        bullet = ratingsMap("bullet"),
        blitz = ratingsMap("blitz"),
        rapid = ratingsMap("rapid"),
        classical = ratingsMap("classical"),
        correspondence = ratingsMap("correspondence"),
        puzzle = Map(
          Standard -> ratingsMap("puzzle"),
          Frisian -> ratingsMap("puzzlefrisian"),
          Russian -> ratingsMap("puzzlerussian")
        )
      )
    }
  }
}
