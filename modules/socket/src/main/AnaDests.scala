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
    lastUci: Option[String] = None,
    fullCapture: Option[Boolean] = None
) {

  def isInitial =
    variant.standard && fen.value == draughts.format.Forsyth.initial && path == ""

  private lazy val sit =
    draughts.DraughtsGame(variant.some, fen.value.some).situation

  private val orig =
    (lastUci.exists(_.length >= 4) && sit.ghosts > 0) ?? lastUci.flatMap { uci =>
      draughts.Pos.posAt(uci.substring(uci.length - 2))
    }

  private lazy val validMoves =
    AnaDests.validMoves(sit, orig, ~fullCapture)

  private lazy val captureLength =
    orig.fold(sit.allMovesCaptureLength)(sit.captureLengthFrom)

  private val truncatedMoves =
    (!isInitial && ~fullCapture && ~captureLength > 1) option AnaDests.truncateMoves(validMoves)

  val dests: String =
    if (isInitial) AnaDests.initialDests
    else sit.playable(false) ?? {
      val truncatedDests = truncatedMoves.map { _ mapValues { _ flatMap (uci => draughts.Pos.posAt(uci.takeRight(2))) } }
      val destStr = destString(truncatedDests.getOrElse(validMoves mapValues { _ map (_.dest) }))
      captureLength.fold(destStr)(capts => "#" + capts.toString + " " + destStr)
    }

  val destsUci: Option[List[String]] =
    truncatedMoves.map(_.values.toList.flatten)

  val alternatives: Option[List[Alternative]] =
    (!isInitial && ~puzzle && sit.ghosts == 0 && ~captureLength > 2) option
      sit.validMovesFinal.values.toList.flatMap(_.map { m =>
        Alternative(
          uci = m.toUci.uci,
          fen = draughts.format.Forsyth.exportBoard(m.after)
        )
      }).take(100)

  lazy val opening = Variant.openingSensibleVariants(variant) ?? {
    FullOpeningDB findByFen fen.value
  }

  def json = Json.obj(
    "dests" -> dests,
    "path" -> path
  ).add("opening" -> opening)
    .add("ch", chapterId)
    .add("alternatives", alternatives)
    .add("destsUci", destsUci)
}

object AnaDests {

  private val initialDests = "HCD GBC ID FAB EzA"

  private def uniqueUci(otherUcis: List[String], uci: String) = {
    var i = 2
    var unique = uci.slice(0, i)
    while (i + 2 <= uci.length && otherUcis.exists(_.startsWith(unique))) {
      i += 2
      unique = uci.slice(0, i)
    }
    unique
  }

  def validMoves(sit: draughts.Situation, from: Option[draughts.Pos], fullCapture: Boolean) =
    from.fold(if (fullCapture) sit.validMovesFinal else sit.validMoves) { pos =>
      Map(pos -> sit.movesFrom(pos, fullCapture))
    }

  def truncateMoves(validMoves: Map[draughts.Pos, List[draughts.Move]]) = {
    var truncated = false
    val truncatedMoves = validMoves map {
      case (pos, moves) =>
        if (moves.size <= 1) pos -> moves.map(_.toUci.uci)
        else pos -> moves.foldLeft(List[String]()) { (acc, move) =>
          val sameDestUcis = moves.filter(m => m != move && m.dest == move.dest).map(_.toUci.uci)
          val uci = move.toUci.uci
          val newUci = if (sameDestUcis.isEmpty) uci else uniqueUci(sameDestUcis, uci)
          if (!acc.contains(newUci)) {
            if (newUci.length != uci.length) truncated = true
            newUci :: acc
          } else {
            truncated = true
            acc
          }
        }
    }
    if (truncated) truncateUcis(truncatedMoves)
    else truncatedMoves
  }

  @scala.annotation.tailrec
  def truncateUcis(validUcis: Map[draughts.Pos, List[String]]): Map[draughts.Pos, List[String]] = {
    var truncated = false
    val truncatedUcis = validUcis map {
      case (pos, uciList) =>
        if (uciList.size <= 1) pos -> uciList
        else pos -> uciList.foldLeft(List[String]()) { (acc, uci) =>
          val sameDestUcis = uciList.filter(u => u != uci && u.takeRight(2) == uci.takeRight(2))
          val newUci = if (sameDestUcis.isEmpty) uci else uniqueUci(sameDestUcis, uci)
          if (!acc.contains(newUci)) {
            if (newUci.length != uci.length) truncated = true
            newUci :: acc
          } else {
            truncated = true
            acc
          }
        }
    }
    if (!truncated) truncateUcis(truncatedUcis)
    else truncatedUcis
  }

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
