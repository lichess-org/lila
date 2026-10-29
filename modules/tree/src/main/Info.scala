package lila.tree

import chess.Ply
import chess.eval.WinPercent
import chess.format.pgn.{ Comment, SanStr }

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
      _ <-
        if (json \ "variation").asOpt[JsArray].exists(_.value.sizeIs > LineMaxPlies) then
          JsError("Too many plies")
        else JsSuccess(())
      ply <- (json \ "ply").validate[Ply]
      score <- readScore(json \ "eval", ply)
      best <- (json \ "eval" \ "best").validateOpt[Uci]
      variation <- (json \ "variation").validate[List[SanStr]]
    yield Info(ply, Eval(score, best), variation)

  def readScore(eval: JsLookupResult, ply: Ply): JsResult[Option[WhiteScore]] =
    for
      cp <- (eval \ "cp").validateOpt[Int]
      mate <- (eval \ "mate").validateOpt[Int]
      mateGiven <- (eval \ "mateGiven").validateOpt[Boolean]
    yield cp
      .map(cp => WhiteScore.fromWhite(Score.cp(cp)))
      .orElse:
        mate.map:
          case 0 => WhiteScore(if mateGiven.has(true) then Score.MateGiven else Score.mated, ply.turn)
          case value => WhiteScore.fromWhite(Score.mate(value))
