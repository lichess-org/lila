package lila.history

import scala.util.Success

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
):

  def apply(perfType: PerfType): RatingsMap =
    perfType match
      case PerfType.Standard       => standard
      case PerfType.Bullet         => bullet
      case PerfType.Blitz          => blitz
      case PerfType.Rapid          => rapid
      case PerfType.Classical      => classical
      case PerfType.Correspondence => correspondence
      case PerfType.Chess960       => chess960
      case PerfType.KingOfTheHill  => kingOfTheHill
      case PerfType.Antichess      => antichess
      case PerfType.ThreeCheck     => threeCheck
      case PerfType.Atomic         => atomic
      case PerfType.Horde          => horde
      case PerfType.RacingKings    => racingKings
      case PerfType.Crazyhouse     => crazyhouse
      case PerfType.Puzzle         => puzzle
      case PerfType.UltraBullet    => ultraBullet

object History:

  import reactivemongo.api.bson.*

  private[history] given ratingsReader: BSONDocumentReader[RatingsMap] with
    def readDocument(doc: BSONDocument) = Success:
      doc.elements
        .flatMap:
          case BSONElement(k, BSONInteger(v)) => k.toIntOption.map(_ -> IntRating(v))
          case _                              => none
        .sortBy(_._1)
        .toList

  private[history] given BSONDocumentReader[History] with
    def readDocument(doc: BSONDocument) = Success:
      def ratingsMap(key: String): RatingsMap = ~doc.getAsOpt[RatingsMap](key)
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
