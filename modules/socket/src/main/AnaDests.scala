package lila.socket

import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaDests(
    variant: chess.variant.Variant,
    fen: String,
    path: String) {

  def isInitial =
    variant.standard && fen == chess.format.Forsyth.initial && path == "0"

  def dests: String =
    if (isInitial) "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"
    else chess.Game(variant.some, fen.some).situation.destinations map {
      case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
    } mkString " "
}

object AnaDests {

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaDests(
    variant = variant,
    fen = fen,
    path = path)
}
