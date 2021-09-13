package shogi
package format
package pgn

import org.joda.time.format.DateTimeFormat

import scala._

case class Pgn(
    tags: Tags,
    turns: List[Turn],
    initial: Initial = Initial.empty
) {

  def updateTurn(fullMove: Int, f: Turn => Turn) = {
    val index = fullMove - 1
    (turns lift index).fold(this) { turn =>
      copy(turns = turns.updated(index, f(turn)))
    }
  }
  def updatePly(ply: Int, f: Move => Move) = {
    val fullMove = (ply + 1) / 2
    val color    = Color(ply % 2 == 1)
    updateTurn(fullMove, _.update(color, f))
  }
  def updateLastPly(f: Move => Move) = updatePly(nbPlies, f)

  def nbPlies = turns.foldLeft(0)(_ + _.count)

  def moves =
    turns.flatMap { t =>
      List(t.sente, t.gote).flatten
    }

  def withEvent(title: String) =
    copy(
      tags = tags + Tag(_.Event, title)
    )

  def render: String = {
    val initStr =
      if (initial.comments.nonEmpty) initial.comments.mkString("{ ", " } { ", " }\n")
      else ""
    val turnStr = turns mkString " "
    val endStr  = tags(_.Result) | ""
    s"$tags\n\n$initStr$turnStr $endStr"
  }.trim

  override def toString = render
}

case class Initial(comments: List[String] = Nil)

object Initial {
  val empty = Initial(Nil)
}

case class Turn(
    number: Int,
    sente: Option[Move],
    gote: Option[Move]
) {

  def update(color: Color, f: Move => Move) =
    color.fold(
      copy(sente = sente map f),
      copy(gote = gote map f)
    )

  def updateLast(f: Move => Move) = {
    gote.map(m => copy(gote = f(m).some)) orElse
      sente.map(m => copy(sente = f(m).some))
  } | this

  def isEmpty = sente.isEmpty && gote.isEmpty

  def plyOf(color: Color) = number * 2 - color.fold(1, 0)

  def count = List(sente, gote) count (_.isDefined)

  override def toString = {
    val text = (sente, gote) match {
      case (Some(s), Some(g)) if s.isLong => s" $s $number... $g"
      case (Some(s), Some(g))             => s" $s $g"
      case (Some(s), None)                => s" $s"
      case (None, Some(g))                => s".. $g"
      case _                              => ""
    }
    s"$number.$text"
  }
}

object Turn {

  def fromMoves(moves: List[Move], ply: Int): List[Turn] = {
    moves.foldLeft((List[Turn](), ply)) {
      case ((turns, p), move) if p % 2 == 1 =>
        (Turn((p + 1) / 2, move.some, none) :: turns) -> (p + 1)
      case ((Nil, p), move) =>
        (Turn((p + 1) / 2, none, move.some) :: Nil) -> (p + 1)
      case ((t :: tt, p), move) =>
        (t.copy(gote = move.some) :: tt) -> (p + 1)
    }
  }._1.reverse
}

case class Move(
    san: String,
    comments: List[String] = Nil,
    glyphs: Glyphs = Glyphs.empty,
    opening: Option[String] = None,
    result: Option[String] = None,
    variations: List[List[Turn]] = Nil,
    // time left for the user who made the move, after he made it
    secondsLeft: Option[Int] = None
) {

  def isLong = comments.nonEmpty || variations.nonEmpty

  private def clockString: Option[String] =
    secondsLeft.map(seconds => "(" + Move.formatPgnSeconds(seconds) + "/)") // todo change to time spent

  override def toString = {
    val glyphStr = glyphs.toList
      .map({
        case glyph if glyph.id <= 6 => glyph.symbol
        case glyph                  => s" $$${glyph.id}"
      })
      .mkString
    val commentsOrTime =
      if (comments.nonEmpty || secondsLeft.isDefined || opening.isDefined || result.isDefined)
        List(clockString, opening, result).flatten
          .:::(comments map Move.noDoubleLineBreak)
          .map { text =>
            s" { $text }"
          }
          .mkString
      else ""
    val variationString =
      if (variations.isEmpty) ""
      else variations.map(_.mkString(" (", " ", ")")).mkString(" ")
    s"$san$glyphStr$commentsOrTime$variationString"
  }
}

object Move {

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def noDoubleLineBreak(txt: String) =
    noDoubleLineBreakRegex.replaceAllIn(txt, "\n")

  private def formatPgnSeconds(t: Int) =
    periodFormatter.print(
      org.joda.time.Duration.standardSeconds(t).toPeriod
    )

  private[this] val periodFormatter = new org.joda.time.format.PeriodFormatterBuilder().printZeroAlways
    .minimumPrintedDigits(1)
    .appendHours
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendMinutes
    .appendSeparator(":")
    .appendSeconds
    .toFormatter

}
