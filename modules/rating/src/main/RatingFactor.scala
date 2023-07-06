package lila.rating

import play.api.data.Form
import play.api.data.Forms.{ single, text }

import lila.memo.SettingStore.{ Formable, StringReader }
import reactivemongo.api.bson.BSONHandler
import lila.common.Iso

opaque type RatingFactor = Double
object RatingFactor extends OpaqueDouble[RatingFactor]:

  private val separator = ","

  private def write(rfs: RatingFactors): String =
    rfs.map { case (pt, f) =>
      s"${pt.key}=$f"
    } mkString separator

  private def read(s: String): RatingFactors =
    s.split(separator).toList.map(_.trim.split('=')) flatMap {
      case Array(ptk, fs) =>
        for
          pt <- PerfType(Perf.Key(ptk))
          f  <- fs.toDoubleOption
        yield pt -> RatingFactor(f)
      case _ => None
    } toMap

  private given Iso.StringIso[RatingFactors] = Iso.string(read, write)

  given BSONHandler[RatingFactors]  = lila.db.dsl.isoHandler
  given StringReader[RatingFactors] = StringReader.fromIso
  given Formable[RatingFactors]     = new Formable(rfs => Form(single("v" -> text)) fill write(rfs))
