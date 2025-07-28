package lila.study

import chess.format.Fen
import chess.json.Json.destString
import chess.variant.Variant
import play.api.libs.json.*

import lila.common.Json.given

case class AnaDests(
    variant: Variant,
    fen: Fen.Full,
    path: String,
    chapterId: Option[StudyChapterId]
):

  def isInitial = variant.standard && fen.isInitial && path == ""

  val dests: String =
    if isInitial then AnaDests.initialDests
    else
      val position = chess.Game(variant.some, fen.some).position
      position.playable(false).so(destString(position.destinations))

  def json = Json
    .obj(
      "dests" -> dests,
      "path" -> path
    )
    .add("ch", chapterId)

object AnaDests:

  private val initialDests = "iqy muC gvx ltB bqs pxF jrz nvD ksA owE"

  def parse(o: JsObject) =
    import lila.common.Json.given
    import chess.variant.Variant
    for
      d <- o.obj("d")
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      fen <- d.get[Fen.Full]("fen")
      path <- d.str("path")
      chapterId = d.get[StudyChapterId]("ch")
    yield AnaDests(variant = variant, fen = fen, path = path, chapterId = chapterId)
