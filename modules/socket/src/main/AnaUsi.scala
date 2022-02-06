package lila.socket

import cats.data.Validated
import shogi.format.forsyth.Sfen
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.opening._
import shogi.variant.Variant
import play.api.libs.json._

import lila.tree.Branch

trait AnaAny {

  def branch: Validated[String, Branch]
  def chapterId: Option[String]
  def path: String
}

case class AnaUsi(
    usi: Usi,
    sfen: Sfen,
    variant: Variant,
    path: String,
    chapterId: Option[String]
) extends AnaAny {

  def branch: Validated[String, Branch] = {
    sfen.toSituationPlus(variant) toValid "Can't parse SFEN" map { parsed =>
      val movable = parsed.situation.playable(strict = false, withImpasse = false)
      val sfen    = parsed.toSfen
      Branch(
        id = UsiCharPair(usi, variant),
        ply = parsed.plies,
        usi = usi,
        sfen = sfen,
        check = parsed.situation.check,
        opening = (parsed.plies <= 30 && Variant.openingSensibleVariants(variant)) ?? {
          FullOpeningDB findBySfen sfen
        }
      )
    }
  }
}

object AnaUsi {

  def parse(o: JsObject) = {
    for {
      d    <- o obj "d"
      usi  <- d str "usi" flatMap shogi.format.usi.Usi.apply
      sfen <- d str "sfen" map Sfen.apply
      path <- d str "path"
    } yield AnaUsi(
      usi = usi,
      variant = shogi.variant.Variant orDefault ~d.str("variant"),
      sfen = sfen,
      path = path,
      chapterId = d str "ch"
    )
  }
}
