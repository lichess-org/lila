package lila.socket

import play.api.libs.json._

import chess.format.FEN
import chess.opening._
import chess.variant.Variant
import lila.common.PimpedJson._
import lila.tree.Node.openingWriter

case class AnaDests(
    variant: Variant,
    fen: FEN,
    path: String) {

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

  def json = Json.obj(
    "dests" -> dests,
    "path" -> path
  ).add("opening", opening)
}

object AnaDests {

  private val initialDests = "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"

  case class Ref(variant: Variant, fen: String, path: String) {

    def compute = AnaDests(variant, FEN(fen), path)
  }

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaDests.Ref(variant = variant, fen = fen, path = path)
}
