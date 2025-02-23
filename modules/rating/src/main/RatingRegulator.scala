package lila.rating

import reactivemongo.api.bson.BSONHandler
import chess.ByColor
import chess.rating.glicko.Glicko
import scalalib.Iso

opaque type RatingFactor = Double
object RatingFactor extends OpaqueDouble[RatingFactor]:

  type ByKey = Map[PerfKey, RatingFactor]

  private val separator = ","

  def write(rfs: ByKey): String =
    rfs
      .map: (pk, f) =>
        s"$pk=$f"
      .mkString(separator)

  private def read(s: String): ByKey =
    s.split(separator)
      .toList
      .map(_.trim.split('='))
      .flatMap:
        case Array(ptk, fs) =>
          for
            pk <- PerfKey(ptk)
            f  <- fs.toDoubleOption
          yield pk -> RatingFactor(f)
        case _ => None
      .toMap

  given Iso.StringIso[ByKey] = Iso.string(read, write)

  given BSONHandler[ByKey] = lila.db.dsl.isoHandler

final class RatingRegulator(factors: RatingFactor.ByKey):

  def apply(
      key: PerfKey,
      before: ByColor[Glicko],
      after: ByColor[Glicko],
      isBot: ByColor[Boolean]
  ): ByColor[Glicko] =
    val regulated = before.zip(after, (b, a) => regulate(key, b, a))
    val halvedAgainstBot = regulated.mapWithColor: (color, glicko) =>
      if !isBot(color) && isBot(!color)
      then glicko.average(before(color))
      else glicko
    halvedAgainstBot

  private def regulate(key: PerfKey, before: Glicko, after: Glicko): Glicko =
    factors
      .get(key)
      .filter(_.value != 1)
      .fold(after):
        regulate(_, key, before, after)

  private def regulate(factor: RatingFactor, key: PerfKey, before: Glicko, after: Glicko): Glicko =
    if after.rating > before.rating
    then
      val diff  = after.rating - before.rating
      val extra = diff * (factor.value - 1)
      lila.mon.rating.regulator.micropoints(key.value).record((extra * 1000 * 1000).toLong)
      after.copy(rating = after.rating + extra)
    else after
