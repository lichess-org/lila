package lidraughts.analyse

import draughts.Color
import draughts.format.Uci

import lidraughts.tree.Eval

case class Info(
    ply: Int,
    eval: Eval,
    // variation is first in UCI, then converted to PDN before storage
    variation: List[String] = Nil
) {

  def cp = eval.cp
  def win = eval.win
  def best = eval.best

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    best ?? (_.piotr),
    variation take Info.LineMaxPlies mkString " ",
    win ?? (_.value.toString),
    cp ?? (_.value.toString)
  ).dropWhile(_.isEmpty).reverse mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, eval = eval.dropBest)

  def invert = copy(eval = eval.invert)

  def cpComment: Option[String] = cp map (_.showPieces)
  def winComment: Option[String] = win map { m => s"Win in ${math.abs(m.value)}" }
  def evalComment: Option[String] = cpComment orElse winComment

  def isEmpty = cp.isEmpty && win.isEmpty

  def forceCentipieces: Option[Int] = win match {
    case None => cp.map(_.centipieces)
    case Some(m) if m.negative => Some(Int.MinValue - m.value)
    case Some(m) => Some(Int.MaxValue - m.value)
  }

  override def toString = s"Info $color [$ply] ${cp.fold("?")(_.showPieces)} ${win.??(_.value)} ${variation.mkString(" ")}"
}

object Info {

  import Eval.{ Cp, Win }

  val LineMaxPlies = 14

  private val separator = ","
  private val listSeparator = ";"

  def start(ply: Int) = Info(ply, Eval.initial, Nil)

  private def strCp(s: String) = parseIntOption(s) map Cp.apply
  private def strWin(s: String) = parseIntOption(s) map Win.apply

  private def decode(ply: Int, str: String): Option[Info] = str.split(separator) match {
    case Array() => Info(ply, Eval.empty).some
    case Array(cp) => Info(ply, Eval(strCp(cp), None, None)).some
    case Array(cp, ma) => Info(ply, Eval(strCp(cp), strWin(ma), None)).some
    case Array(cp, ma, va) => Info(ply, Eval(strCp(cp), strWin(ma), None), va.split(' ').toList).some
    case Array(cp, ma, va, be) => Info(ply, Eval(strCp(cp), strWin(ma), Uci.Move piotr be), va.split(' ').toList).some
    case _ => none
  }

  def decodeList(str: String, fromPly: Int): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) => decode(index + 1 + fromPly, infoStr)
    }
  }.sequence

  def encodeList(infos: List[Info]): String = infos map (_.encode) mkString listSeparator

  def apply(cp: Option[Cp], win: Option[Win], variation: List[String]): Int => Info =
    ply => Info(ply, Eval(cp, win, None), variation)
}
