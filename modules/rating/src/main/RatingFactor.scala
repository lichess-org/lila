package lila.rating

import play.api.data.Form
import play.api.data.Forms.{ single, text }

import lila.memo.SettingStore.{ Formable, StringReader }
import reactivemongo.api.bson.BSONHandler

case class RatingFactor(value: Double) extends AnyVal with DoubleValue

object RatingFactor:

  private val separator = ","

  private def write(rfs: RatingFactors): String =
    rfs.map { case (pt, f) =>
      s"${pt.key}=$f"
    } mkString separator

  private def read(s: String): RatingFactors =
    s.split(separator).toList.map(_.trim.split('=')) flatMap {
      case Array(ptk, fs) =>
        for {
          pt <- PerfType(ptk)
          f  <- fs.toDoubleOption
        } yield pt -> RatingFactor(f)
      case _ => None
    } toMap

  private val ratingFactorsIso = lila.common.Iso[String, RatingFactors](
    str => read(str),
    rf => write(rf)
  )

  given BSONHandler[RatingFactors]  = lila.db.dsl.isoHandler
  given StringReader[RatingFactors] = StringReader.fromIso(ratingFactorsIso)
  given Formable[RatingFactors] =
    new Formable[RatingFactors](rfs => Form(single("v" -> text)) fill write(rfs))
