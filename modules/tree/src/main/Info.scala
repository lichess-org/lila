package lila.tree

import chess.Ply
import chess.format.Uci
import chess.format.pgn.{ Comment, SanStr }
import chess.eval.WinPercent

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
      mate.so(_.value.toString),
      cp.so(_.value.toString)
    ).dropWhile(_.isEmpty).reverse.mkString(Info.separator)

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, eval = eval.dropBest)

  def invert = copy(eval = eval.invert)

  def cpComment: Option[String] = cp.map(_.showPawns)
  def mateComment: Option[String] =
    mate.map: m =>
      s"Mate in ${math.abs(m.value)}"
  // advise comment
  def evalComment: Option[String] = cpComment.orElse(mateComment)

  // pgn comment
  def pgnComment = Comment.from(
    cp
      .map(_.pawns.toString)
      .orElse(mate.map(m => s"#${m.value}"))
      .map(c => s"[%eval $c]")
  )

  def isEmpty = cp.isEmpty && mate.isEmpty

  override def toString =
    s"Info $color [$ply] ${cp.fold("?")(_.showPawns)} ${mate.so(_.value)} $best"

object Info:

  import chess.eval.Eval.{ Cp, Mate }

  val LineMaxPlies = 12

  private val separator = ","
  private val listSeparator = ";"

  def start(ply: Ply) = Info(ply, evals.initial, Nil)

  private def strCp(s: String) = Cp.from(s.toIntOption)
  private def strMate(s: String) = Mate.from(s.toIntOption)

  private def decode(ply: Ply, str: String): Option[Info] =
    str.split(separator) match
      case Array() => Info(ply, evals.empty, Nil).some
      case Array(cp) => Info(ply, Eval(strCp(cp), None, None), Nil).some
      case Array(cp, ma) => Info(ply, Eval(strCp(cp), strMate(ma), None), Nil).some
      case Array(cp, ma, va) =>
        Info(ply, Eval(strCp(cp), strMate(ma), None), SanStr.from(va.split(' ').toList)).some
      case Array(cp, ma, va, be) =>
        Info(
          ply,
          Eval(strCp(cp), strMate(ma), Uci.Move.fromChars(be)),
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

  def apply(cp: Option[Cp], mate: Option[Mate], variation: List[SanStr]): Ply => Info =
    ply => Info(ply, Eval(cp, mate, None), variation)
