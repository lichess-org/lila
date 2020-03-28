package lidraughts.socket

import play.api.libs.json._
import draughts.format.FEN
import draughts.opening._
import draughts.variant.Variant
import lidraughts.tree.Node.{ Alternative, alternativeWriter, destString, openingWriter }
import ornicar.scalalib.Zero

case class AnaDests(
    variant: Variant,
    fen: FEN,
    path: String,
    chapterId: Option[String],
    puzzle: Option[Boolean],
    uci: Option[String] = None,
    fullCapture: Option[Boolean] = None
) {

  def isInitial =
    variant.standard && fen.value == draughts.format.Forsyth.initial && path == ""

  val sit = draughts.DraughtsGame(variant.some, fen.value.some).situation

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else {
      sit.playable(false) ?? {
        (sit.ghosts > 0 && uci.exists(_.length >= 4)) ?? uci.flatMap { u =>
          draughts.Pos.posAt(u.substring(u.length - 2))
        } match {
          case Some(pos) =>
            val destStr = destString(Map(pos -> sit.destinationsFrom(pos, ~fullCapture)))
            sit.captureLengthFrom(pos).fold(destStr)(capts => "#" + capts.toString + " " + destStr)
          case _ =>
            val destStr = destString(if (~fullCapture) sit.allDestinationsFinal else sit.allDestinations)
            sit.allMovesCaptureLength.fold(destStr)(capts => "#" + capts.toString + " " + destStr)
        }
      }
    }

  val alternatives: Option[List[Alternative]] =
    if ((~fullCapture && ~sit.allMovesCaptureLength > 0) || (~puzzle && sit.ghosts == 0 && ~sit.allMovesCaptureLength > 2))
      sit.validMovesFinal.values.toList.flatMap(_.map { m =>
        Alternative(
          uci = m.toUci.uci,
          fen = ~puzzle option draughts.format.Forsyth.exportBoard(m.after)
        )
      }).take(100).some
    else none

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen.value
  }

  def json = Json.obj(
    "dests" -> dests,
    "path" -> path
  ).add("opening" -> opening)
    .add("ch", chapterId)
    .add("alternatives", alternatives)
}

object AnaDests {

  private val initialDests = "HCD GBC ID FAB EzA"

  def parse(o: JsObject) = for {
    d ← o obj "d"
    variant = draughts.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaDests(
    variant = variant,
    fen = FEN(fen),
    path = path,
    chapterId = d str "ch",
    puzzle = d boolean "puzzle",
    fullCapture = d boolean "fullCapture"
  )
}
