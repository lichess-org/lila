package lila.history

import scala.util.Success

import lila.rating.PerfType

case class History(
    standard: RatingsMap,
    minishogi: RatingsMap,
    chushogi: RatingsMap,
    annanshogi: RatingsMap,
    kyotoshogi: RatingsMap,
    checkshogi: RatingsMap,
    ultraBullet: RatingsMap,
    bullet: RatingsMap,
    blitz: RatingsMap,
    rapid: RatingsMap,
    classical: RatingsMap,
    correspondence: RatingsMap,
    puzzle: RatingsMap
) {

  def apply(perfType: PerfType): RatingsMap =
    perfType match {
      case PerfType.Standard       => standard
      case PerfType.Bullet         => bullet
      case PerfType.Blitz          => blitz
      case PerfType.Rapid          => rapid
      case PerfType.Classical      => classical
      case PerfType.Correspondence => correspondence
      case PerfType.Puzzle         => puzzle
      case PerfType.UltraBullet    => ultraBullet
      case PerfType.Minishogi      => minishogi
      case PerfType.Chushogi       => chushogi
      case PerfType.Annanshogi     => annanshogi
      case PerfType.Kyotoshogi     => kyotoshogi
      case PerfType.Checkshogi     => checkshogi
      case x                       => sys error s"No history for perf $x"
    }
}

object History {

  import reactivemongo.api.bson._

  implicit private[history] val RatingsMapReader = new BSONDocumentReader[RatingsMap] {
    def readDocument(doc: BSONDocument) =
      Success(
        doc.elements
          .flatMap {
            case BSONElement(k, BSONInteger(v)) => k.toIntOption map (_ -> v)
            case _                              => none[(Int, Int)]
          }
          .sortBy(_._1)
          .toList
      )
  }

  implicit private[history] val HistoryBSONReader = new BSONDocumentReader[History] {
    def readDocument(doc: BSONDocument) =
      Success {
        def ratingsMap(key: String): RatingsMap = ~doc.getAsOpt[RatingsMap](key)
        History(
          standard = ratingsMap("standard"),
          minishogi = ratingsMap("minishogi"),
          chushogi = ratingsMap("chushogi"),
          annanshogi = ratingsMap("annanshogi"),
          kyotoshogi = ratingsMap("kyotoshogi"),
          checkshogi = ratingsMap("checkshogi"),
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
