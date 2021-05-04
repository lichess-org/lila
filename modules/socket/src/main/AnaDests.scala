package lila.socket

import play.api.libs.json._

import shogi.format.FEN
import shogi.opening._
import shogi.variant.Variant
import lila.tree.Node.{ destString, openingWriter }

case class AnaDests(
    variant: Variant,
    fen: FEN,
    path: String,
    chapterId: Option[String]
) {

  def isInitial =
    variant.standard && fen.value == shogi.format.Forsyth.initial && path == ""

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else {
      val sit = shogi.Game(variant.some, fen.value.some).situation
      sit.playable(false) ?? destString(sit.destinations)
    }

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen
  }

  def json =
    Json
      .obj(
        "dests" -> dests,
        "path"  -> path
      )
      .add("opening" -> opening)
      .add("ch", chapterId)
}

object AnaDests {

  private val initialDests = "aj fonp uD gpo vE clm wF dmln sB qponmlr ir tC enmo yH zI AJ xG"

  def parse(o: JsObject) =
    for {
      d <- o obj "d"
      variant = shogi.variant.Variant orDefault ~d.str("variant")
      fen  <- d str "fen"
      path <- d str "path"
    } yield AnaDests(variant = variant, fen = FEN(fen), path = path, chapterId = d str "ch")
}
