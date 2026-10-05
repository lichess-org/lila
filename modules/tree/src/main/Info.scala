package lila.tree

import chess.Ply
import chess.format.pgn.{ Comment, SanStr }
import chess.eval.WinPercent

// ply AFTER the move was played
case class Info(ply: Ply, eval: Eval, variation: List[SanStr]):

  export eval.{ cp, mate, best }

  def prevPly: Ply = ply - 1
  def prevMoveNumber = prevPly.fullMoveNumber
  def color = prevPly.turn

  def winPercent = eval.cp.map(WinPercent.fromCentiPawns)

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, eval = eval.dropBest)

  def exportMate: Option[Int] = eval.score.flatMap(_.exportMate(ply.turn))

  private def cpComment: Option[String] = cp.map(_.showPawns)
  private def mateComment: Option[String] =
    mate
      .filter(_.value != 0)
      .map: m =>
        s"Mate in ${math.abs(m.value)}"
  // advise comment
  def evalComment: Option[String] = cpComment.orElse(mateComment)

  // pgn comment
  def pgnComment = Comment.from(
    cp.map(_.pawns.toString)
      .orElse(exportMate.map(m => s"#$m"))
      .map(c => s"[%eval $c]")
  )

  def isEmpty = eval.isEmpty

  override def toString =
    s"Info $color [$ply] ${cp.fold("?")(_.showPawns)} ${mate.so(_.value)} $best"

object Info:

  val LineMaxPlies = 12

  def start(ply: Ply) = Info(ply, evals.initial, Nil)

  import play.api.libs.json.*
  import scalalib.json.Json.given
  import chess.json.Json.given
  import chess.eval.{ Score, WhiteScore }
  import chess.format.Uci

  given Reads[Ply] = Reads:
    case JsNumber(n) => JsSuccess(Ply(n.toInt))
    case _ => JsError("Ply must be int")
  given Reads[Info] = Reads: json =>
    for
      ply <- (json \ "ply").validate[Ply]
      cp <- (json \ "eval" \ "cp").validateOpt[Int]
      mate <- (json \ "eval" \ "mate").validateOpt[Int]
      best <- (json \ "eval" \ "best").validateOpt[Uci]
      variation <- (json \ "variation").validate[List[SanStr]]
    yield
      val score = cp
        .map(Score.cp)
        .orElse:
          mate.map: value =>
            if value == 0 && ply.turn.black then Score.MateGiven else Score.mate(value)
      Info(ply, Eval(score.map(WhiteScore.fromWhite), best), variation)
