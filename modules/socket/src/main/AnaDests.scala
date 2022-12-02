package lila.socket

import play.api.libs.json.*

import chess.format.Fen
import chess.opening.*
import chess.variant.Variant
import lila.tree.Node.{ destString, openingWriter }
import lila.common.Json.given

case class AnaDests(
    variant: Variant,
    fen: Fen,
    path: String,
    chapterId: Option[StudyChapterId]
):

  def isInitial = variant.standard && fen.isInitial && path == ""

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else
      val sit = chess.Game(variant.some, fen.some).situation
      sit.playable(false) ?? destString(sit.destinations)

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    OpeningDb findByFen fen.opening
  }

  def json =
    Json
      .obj(
        "dests" -> dests,
        "path"  -> path
      )
      .add("opening" -> opening)
      .add("ch", chapterId)

object AnaDests:

  private val initialDests = "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"

  def parse(o: JsObject) =
    import lila.common.Json.given
    for {
      d <- o obj "d"
      variant = chess.variant.Variant orDefault ~d.str("variant")
      fen  <- d.get[Fen]("fen")
      path <- d str "path"
      chapterId = d.get[StudyChapterId]("ch")
    } yield AnaDests(variant = variant, fen = fen, path = path, chapterId = chapterId)
