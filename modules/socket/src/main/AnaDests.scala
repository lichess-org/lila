package lila.socket

import chess.opening._
import chess.variant.Variant
import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaDests(
    variant: Variant,
    fen: String,
    path: String) {

  def isInitial =
    variant.standard && fen == chess.format.Forsyth.initial && path == ""

  val dests: String =
    if (isInitial) "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"
    else chess.Game(variant.some, fen.some).situation.destinations map {
      case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
    } mkString " "

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen
  }
}

object AnaDests {

  case class Ref(
      variant: Variant,
      fen: String,
      path: String) {

    def compute = AnaDests(variant, fen, path)
  }

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaDests.Ref(variant = variant, fen = fen, path = path)
}
