package lila.tree

import chess.Ply
import chess.format.Uci
import chess.format.pgn.{ Comment, SanStr }
import chess.eval.{ Score, WinPercent }

// ply AFTER the move was played
case class Info(ply: Ply, eval: Eval, variation: List[SanStr]):

  export eval.{ cp, mate, best }

  def prevPly: Ply = ply - 1
  def prevMoveNumber = prevPly.fullMoveNumber
  def color = prevPly.turn

  def winPercent = eval.cp.map(WinPercent.fromCentiPawns)

  def encode: String =
    List(
      best.so(_.chars),
      variation.take(Info.LineMaxPlies).mkString(" "),
      eval.score.so(_.white.fold(_ => "", Info.encodeMate, Info.mateGiven)),
      cp.so(_.value.toString)
    ).dropWhile(_.isEmpty).reverse.mkString(Info.separator)

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

  import chess.eval.WhiteScore
  import chess.eval.Eval.{ Cp, Mate }

  val LineMaxPlies = 12

  private val separator = ","
  private val listSeparator = ";"

  def start(ply: Ply) = Info(ply, evals.initial, Nil)

  // Always with explicit +/-. "0" is the legacy encoding of Mate(0).
  private val mateGiven = "+0"
  private def encodeMate(m: Mate) = if m.value == 0 then "-0" else m.value.toString

  private def strScore(cp: String, mate: String): Option[WhiteScore] =
    Cp.from(cp.toIntOption)
      .map(Score.Cp(_))
      .orElse:
        if mate == mateGiven then Score.MateGiven.some
        else Mate.from(mate.toIntOption).map(Score.Mate(_))
      .map(WhiteScore.fromWhite(_))

  private def decode(ply: Ply, str: String): Option[Info] =
    str.split(separator) match
      case Array() => Info(ply, evals.empty, Nil).some
      case Array(cp) => Info(ply, Eval(strScore(cp, ""), None), Nil).some
      case Array(cp, ma) => Info(ply, Eval(strScore(cp, ma), None), Nil).some
      case Array(cp, ma, va) =>
        Info(ply, Eval(strScore(cp, ma), None), SanStr.from(va.split(' ').toList)).some
      case Array(cp, ma, va, be) =>
        Info(
          ply,
          Eval(strScore(cp, ma), Uci.Move.fromChars(be)),
          SanStr.from(va.split(' ').toList)
        ).some
      case _ => none

  def decodeList(str: String, fromPly: Ply): Option[List[Info]] =
    str
      .split(listSeparator)
      .toList
      .zipWithIndex
      .traverse((infoStr, index) => decode(fromPly + index + 1, infoStr))

  def encodeList(infos: List[Info]): String = infos.map(_.encode).mkString(listSeparator)
