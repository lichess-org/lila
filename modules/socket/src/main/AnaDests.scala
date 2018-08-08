package lidraughts.socket

import play.api.libs.json._

import draughts.format.FEN
import draughts.opening._
import draughts.variant.Variant
import lidraughts.tree.Node.openingWriter
import ornicar.scalalib.Zero

case class AnaDests(
    variant: Variant,
    fen: FEN,
    path: String,
    chapterId: Option[String]
) {

  def isInitial =
    variant.standard && fen.value == draughts.format.Forsyth.initial && path == ""

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else {
      val sit = draughts.DraughtsGame(variant.some, fen.value.some).situation
      sit.playable(false) ?? {
        val destStr = sit.allDestinations map {
          case (orig, thedests) => s"${orig.piotr}${thedests.distinct.map(_.piotr).mkString}"
        } mkString " "
        sit.allMovesCaptureLength.fold(destStr)(capts => "#" + capts.toString + " " + destStr)
      }
    }

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen.value
  }

  def json = Json.obj(
    "dests" -> dests,
    "path" -> path
  ).add("opening" -> opening).add("ch", chapterId)
}

object AnaDests {

  private val initialDests = "HCD GBC ID FAB EzA"

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = draughts.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
    chapterId = d str "ch"
  } yield AnaDests(variant = variant, fen = FEN(fen), path = path, chapterId = chapterId)
}
