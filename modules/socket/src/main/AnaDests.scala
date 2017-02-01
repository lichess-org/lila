package lila.socket

import chess.opening._
import chess.format.FEN
import chess.variant.Variant
import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaDests(
    variant: Variant,
    fen: FEN,
    path: String,
    multiPv: Option[Int]) {

  def isInitial =
    variant.standard && fen.value == chess.format.Forsyth.initial && path == ""

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else {
      val sit = chess.Game(variant.some, fen.value.some).situation
      sit.playable(false) ?? {
        sit.destinations map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        } mkString " "
      }
    }

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen.value
  }
}

object AnaDests {

  private val initialDests = "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"

  case class Ref(
      variant: Variant,
      fen: String,
      path: String,
      multiPv: Option[Int]) {

    def compute = AnaDests(variant, FEN(fen), path, multiPv)
  }

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
    multiPv = d int "multiPv"
  } yield AnaDests.Ref(variant = variant, fen = fen, path = path, multiPv = multiPv)
}
