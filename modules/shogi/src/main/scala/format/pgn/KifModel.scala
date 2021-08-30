package shogi
package format
package pgn

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala._

// This is temporary for exporting KIFs
// The plan is to get completely rid of pgn/san
// and not work around it like we do here
// But I want to do that gradually - #407

case class Kif(
    tags: Tags,
    moves: List[KifMove],
    initial: Initial = Initial.empty,
    variant: shogi.variant.Variant = shogi.variant.Standard
) {
  def updatePly(ply: Int, f: KifMove => KifMove) = {
    val index = ply - 1
    (moves lift index).fold(this) { move =>
      copy(moves = moves.updated(index, f(move)))
    }
  }

  def nbPlies = moves.size

  def updateLastPly(f: KifMove => KifMove) = updatePly(nbPlies, f)
  
  def withEvent(title: String) =
    copy(
      tags = tags + Tag(_.Event, title)
    )

  def renderMoveLine(moveline: List[KifMove]): String =
    moveline.foldLeft((List[String](), None: Option[Pos])) { case ((acc, lastDest), cur) =>
        (acc :+ cur.render(lastDest), cur.dest)
    }._1.map("   " + _) mkString "\n"

  def renderVariationLine(moveline: List[KifMove]): String = {
    val vHeader = moveline.headOption.fold("")( m => s"\n\n変化：${m.ply}手\n")
    val main = renderMoveLine(moveline)
    val variations = moveline.reverse.foldLeft("")((acc: String, cur) => {
      acc + cur.variations.map(v => renderVariationLine(v)).mkString("\n")
    })
    s"$vHeader$main$variations"
  } 

  def render: String = {
    val initStr =
      if (initial.comments.nonEmpty) initial.comments.mkString("* ", "\n" , "\n")
      else ""
    val header = KifUtils kifHeader tags
    val movesHeader    = "\n手数----指手---------消費時間--\n"
    val movesStr = renderMoveLine(moves)
    val variations = moves.reverse.foldLeft("")((acc, cur) => {
        acc + cur.variations.map(v => renderVariationLine(v)).mkString("\n")
    })
    val endStr  = tags(_.Termination) | ""
    s"$header$movesHeader$initStr$movesStr$endStr$variations"
  }.trim

  override def toString = render
}

case class KifMove(
    san: String,
    uci: String,
    ply: Int,
    comments: List[String] = Nil,
    result: Option[String] = None,
    variations: List[List[KifMove]] = Nil,
    // time left for the user who made the move, after he made it
    secondsSpent: Option[Int] = None
) {

  private def clockString: Option[String] =
    secondsSpent.map(seconds => "   (" + KifMove.formatKifSeconds(seconds) + "/" + ")\n")

  def render(lastDest: Option[Pos]) = {
    val kifMove = KifUtils.moveKif(uci, san, lastDest) 
    val timeStr = clockString.getOrElse("")
    val commentsStr = comments.map { text => s"\n* ${KifMove.fixComment(text)}" }.mkString("")
    s"$ply $kifMove$timeStr$commentsStr"
  }

  def dest: Option[Pos] = Uci(uci).map(_.origDest._2)

}

object KifMove {

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def fixComment(txt: String) =
    noDoubleLineBreakRegex.replaceAllIn(txt, "\n").replace("\n", "\n*  ")

  private def formatKifSeconds(t: Int) =
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
