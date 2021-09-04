package shogi
package format
package pgn

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

  def renderMovesAndVariations(moveline: List[KifMove]): String = {
    val mainline = moveline.foldLeft((List[String](), None: Option[Pos])) { case ((acc, lastDest), cur) =>
      (acc :+ (cur.render(lastDest)), cur.dest)
    }._1 mkString "\n"

    val variations = moveline.reverse.foldLeft("")((acc, cur) => {
      acc + cur.variations.map(v => s"\n\n変化：${cur.ply}手\n" + renderMovesAndVariations(v)).mkString("\n")
    })

    s"$mainline$variations"
  }

  def render: String = {
    val initStr =
      if (initial.comments.nonEmpty) initial.comments.mkString("* ", "\n" , "\n")
      else ""
    val header = KifUtils kifHeader tags
    val movesHeader    = "\n手数----指手---------消費時間--\n"
    val movesStr = renderMovesAndVariations(moves)
    s"$header$movesHeader$initStr$movesStr"
  }.trim

  override def toString = render
}

case class KifMove(
    ply: Int,
    uci: String,
    san: String,
    comments: List[String] = Nil,
    glyphs: Glyphs = Glyphs.empty, // treat as comments for now?
    result: Option[String] = None,
    variations: List[List[KifMove]] = Nil,
    // time left for the user who made the move, after he made it
    secondsSpent: Option[Int] = None,
    // total time spent playing so far
    secondsTotal: Option[Int] = None,
) {

  private def clockString: Option[String] =
    secondsSpent.map(spent =>
        s"${KifMove.offset}(${KifMove.formatKifSpent(spent)}/${secondsTotal.fold("")(total => KifMove.formatKifTotal(total))})"
    )

  def render(lastDest: Option[Pos]) = {
    val resultStr = result.fold("")(r => s"\n${KifMove.offset}${ply+1}${KifMove.offset}$r")
    val kifMove = KifUtils.moveKif(uci, san, lastDest) 
    val timeStr = clockString.getOrElse("")
    val glyphsNames = glyphs.toList.map(_.name)
    val commentsStr = (glyphsNames ::: comments).map { text => s"\n* ${KifMove.fixComment(text)}" }.mkString("")
    s"${KifMove.offset}$ply${KifMove.offset}$kifMove$timeStr$commentsStr$resultStr"
  }

  def dest: Option[Pos] = Uci(uci).map(_.origDest._2)

}

object KifMove {

  val offset = "   "

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def fixComment(txt: String) =
    noDoubleLineBreakRegex.replaceAllIn(txt, "\n").replace("\n", "\n* ")

  private def formatKifSpent(t: Int) =
    ms.print(
      org.joda.time.Duration.standardSeconds(t).toPeriod
    )

  private def formatKifTotal(t: Int) =
    hms.print(
      org.joda.time.Duration.standardSeconds(t).toPeriod
    )

  private[this] val ms = new org.joda.time.format.PeriodFormatterBuilder().printZeroAlways
    .minimumPrintedDigits(2)
    .appendMinutes
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendSeconds
    .toFormatter

  private[this] val hms = new org.joda.time.format.PeriodFormatterBuilder().printZeroAlways
    .minimumPrintedDigits(2)
    .appendHours
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendMinutes
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendSeconds
    .toFormatter

}
