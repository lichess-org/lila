package lila.rating

import reactivemongo.api.bson.BSONHandler

import scalalib.Iso
import lila.core.perf.{ PerfType, PerfKey }

opaque type RatingFactor = Double
object RatingFactor extends OpaqueDouble[RatingFactor]:

  private val separator = ","

  def write(rfs: RatingFactors): String =
    rfs
      .map: (pt, f) =>
        s"${pt.key}=$f"
      .mkString(separator)

  private def read(s: String): RatingFactors =
    s.split(separator)
      .toList
      .map(_.trim.split('='))
      .flatMap {
        case Array(ptk, fs) =>
          for
            pt <- PerfType(PerfKey(ptk))
            f  <- fs.toDoubleOption
          yield pt -> RatingFactor(f)
        case _ => None
      } toMap

  given Iso.StringIso[RatingFactors] = Iso.string(read, write)

  given BSONHandler[RatingFactors] = lila.db.dsl.isoHandler
