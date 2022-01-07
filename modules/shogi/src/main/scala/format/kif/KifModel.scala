package shogi
package format
package kif

import cats.syntax.option._
import variant._
import shogi.format.usi.Usi

case class Kif(
    tags: Tags,
    moves: List[NotationMove],
    initial: Initial = Initial.empty
) extends Notation {

  def withMoves(moves: List[NotationMove]) =
    copy(moves = moves)

  def withTags(tags: Tags) =
    copy(tags = tags)

  def renderMovesAndVariations(moveline: List[NotationMove]): String = {
    val mainline = moveline
      .foldLeft((List[String](), None: Option[Pos])) { case ((acc, lastDest), cur) =>
        ((Kif.renderNotationMove(cur, lastDest) :: acc), cur.usiWithRole.usi.origDest._2.some)
      }
      ._1
      .reverse mkString "\n"

    val variations = moveline.reverse.foldLeft("")((acc, cur) => {
      acc + cur.variations.map(v => s"\n\n変化：${cur.moveNumber}手\n${renderMovesAndVariations(v)}").mkString("")
    })

    s"$mainline$variations"
  }

  def render: String = {
    val initStr =
      if (initial.comments.nonEmpty) initial.comments.map(Kif.fixComment _).mkString("* ", "\n* ", "\n")
      else ""
    val header      = Kif renderHeader tags
    val movesHeader = "\n手数----指手---------消費時間--\n"
    val movesStr    = renderMovesAndVariations(moves)
    s"$header$movesHeader$initStr$movesStr"
  }.trim

  override def toString = render
}

object Kif {

  def renderNotationMove(cur: NotationMove, lastDest: Option[Pos]): String = {
    val resultStr   = cur.result.fold("")(r => s"\n${moveNumberOffset(cur.moveNumber + 1)}$offset$r")
    val kifMove     = renderKifMove(cur.usiWithRole, lastDest)
    val timeStr     = clockString(cur).getOrElse("")
    val glyphsNames = cur.glyphs.toList.map(_.name)
    val commentsStr = (glyphsNames ::: cur.comments).map { text => s"\n* ${fixComment(text)}" }.mkString("")
    s"${moveNumberOffset(cur.moveNumber)}$offset$kifMove$timeStr$commentsStr$resultStr"
  }

  def renderKifMove(usiWithRole: Usi.WithRole, lastDest: Option[Pos]): String =
    usiWithRole.usi match {
      case Usi.Drop(role, pos) =>
        s"${makeDestSquare(pos)}${role.kif.head}打"
      case Usi.Move(orig, dest, prom) => {
        val destStr = if (lastDest.fold(false)(_ == dest)) "同　" else makeDestSquare(dest)
        val promStr = if (prom) "成" else ""
        val roleStr = usiWithRole.role.kif.head
        s"$destStr$roleStr$promStr(${orig.numberKey})"
      }
    }

  def renderHeader(tags: Tags): String =
    kifHeaderTags
      .map { tag =>
        // we need these even empty
        if (tag == Tag.Sente || tag == Tag.Gote) {
          val playerName = tags(tag.name).getOrElse("")
          val playerTag = {
            if (!StartingPosition.isFENHandicap(tags.fen)) tag.kifName
            else if (tag == Tag.Sente) "下手"
            else "上手"
          }
          s"$playerTag：${if (playerName == "?") "" else playerName}"
        } else if (tag == Tag.Handicap) {
          renderSetup(tags.variant | Standard, tags.fen)
        } else {
          tags(tag.name).fold("")(tagValue => {
            if (tagValue != "?" && tagValue != "") s"${tag.kifName}：$tagValue"
            else ""
          })
        }
      }
      .filter(_.nonEmpty)
      .mkString("\n")

  def renderSetup(variant: Variant, fen: Option[FEN]): String =
    fen
      .filterNot(f => Forsyth.compareTruncated(variant.initialFen, f.value))
      .fold {
        val handicapName = KifUtils.defaultHandicaps.get(variant).flatMap(_.headOption).getOrElse("")
        s"${Tag.Handicap.kifName}：$handicapName"
      } { fen =>
        getHandicapName(fen).fold((Forsyth << fen.value).fold("")(renderSituation _))(hc =>
          s"${Tag.Handicap.kifName}：$hc"
        )
      }

  def renderSituation(sit: Situation): String = {
    val kifBoard = new scala.collection.mutable.StringBuilder(256)
    val nbRanks  = sit.board.variant.numberOfRanks
    val nbFiles  = sit.board.variant.numberOfFiles
    for (y <- 1 to nbRanks) {
      kifBoard append "|"
      for (x <- nbFiles to 1 by -1) {
        sit.board(x, y) match {
          case None => kifBoard append " ・"
          case Some(piece) =>
            kifBoard append piece.kif
        }
      }
      kifBoard append s"|${KifUtils.intToKanji(y)}"
      if (y < nbRanks) kifBoard append '\n'
    }
    List(
      sit.board.handData.fold("")(hs => "後手の持駒：" + renderHand(hs(Gote))),
      s" ${" ９ ８ ７ ６ ５ ４ ３ ２ １".takeRight(nbFiles * 2)}",
      s"+${"-" * (nbFiles * 3)}+",
      kifBoard.toString,
      s"+${"-" * (nbFiles * 3)}+",
      sit.board.handData.fold("")(hs => "先手の持駒：" + renderHand(hs(Sente))),
      if (sit.color == Gote) "後手番" else ""
    ).filter(_.nonEmpty).mkString("\n")
  }

  private def renderHand(hand: Hand): String = {
    if (hand.size == 0) "なし"
    else
      Standard.handRoles
        .map { r =>
          val cnt = hand(r)
          if (cnt == 1) r.kif.head
          else if (cnt > 1) r.kif.head + KifUtils.intToKanji(cnt)
          else ""
        }
        .filter(_.nonEmpty)
        .mkString("　")
  }

  def createTerminationMove(status: Status, winnerTurn: Boolean): Option[String] = {
    import Status._
    status match {
      case Aborted | NoStart                                                         => "中断".some
      case Timeout | Outoftime                                                       => "切れ負け".some
      case Resign if !winnerTurn                                                     => "投了".some
      case PerpetualCheck                                                            => "反則勝ち".some
      case Mate if winnerTurn                                                        => "反則勝ち".some // pawn checkmate
      case Mate | Stalemate                                                          => "詰み".some
      case Draw                                                                      => "千日手".some
      case Impasse27                                                                 => "入玉勝ち".some
      case Created | Started | UnknownFinish | VariantEnd | TryRule | Cheat | Resign => None
      case _                                                                         => None
    }
  }

  // tags we render in header
  private val kifHeaderTags = Tag.tsumeTypes ++ List(
    Tag.Start,
    Tag.End,
    Tag.Event,
    Tag.Site,
    Tag.TimeControl,
    Tag.Handicap,
    Tag.Sente,
    Tag.SenteTeam,
    Tag.Gote,
    Tag.GoteTeam,
    Tag.Opening
  )

  private def makeDestSquare(sq: Pos): String =
    s"${((sq.x) + 48 + 65248).toChar}${KifUtils.intToKanji(sq.y)}"

  private def getHandicapName(fen: FEN): Option[String] =
    StartingPosition.searchHandicapByFen(fen.some).map(t => t.eco)

  private def clockString(cur: NotationMove): Option[String] =
    cur.secondsSpent.map(spent =>
      s"${offset}(${formatKifSpent(spent)}/${cur.secondsTotal.fold("")(total => formatKifTotal(total))})"
    )

  private val offset = "   "

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def moveNumberOffset(moveNumber: Int) =
    f"$moveNumber%4d"

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
