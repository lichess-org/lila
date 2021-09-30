package shogi
package format
package kif

import scala._

// This is temporary for exporting KIFs
// The plan is to get completely rid of pgn/san
// and not work around it like we do here
// But I want to do that gradually - #407

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
        ((Kif.renderNotationMove(cur, lastDest) :: acc), cur.dest.some)
      }
      ._1
      .reverse mkString "\n"

    val variations = moveline.reverse.foldLeft("")((acc, cur) => {
      acc + cur.variations.map(v => s"\n\n変化：${cur.ply}手\n${renderMovesAndVariations(v)}").mkString("")
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
    val resultStr   = cur.result.fold("")(r => s"\n${plyOffset(cur.ply + 1)}$offset$r")
    val kifMove     = renderKifMove(cur.uci, cur.san, lastDest)
    val timeStr     = clockString(cur).getOrElse("")
    val glyphsNames = cur.glyphs.toList.map(_.name)
    val commentsStr = (glyphsNames ::: cur.comments).map { text => s"\n* ${fixComment(text)}" }.mkString("")
    s"${plyOffset(cur.ply)}$offset$kifMove$timeStr$commentsStr$resultStr"
  }

  def renderKifMove(uci: Uci, san: String, lastDest: Option[Pos]): String =
    uci match {
      case Uci.Drop(role, pos) =>
        s"${makeDestSquare(pos)}${role.kif}打"
      case Uci.Move(orig, dest, prom) => {
        val destStr = if (lastDest.fold(false)(_ == dest)) "同　" else makeDestSquare(dest)
        val promStr = if (prom) "成" else ""
        san.headOption.flatMap(s => Role.allByPgn.get(s)).fold(s"move parse error - $uci, $san") { r =>
          s"$destStr${r.kif}$promStr(${makeOrigSquare(orig)})"
        }
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
          renderSetup(tags.fen)
        } else {
          tags(tag.name).fold("")(tagValue => {
            if (tagValue != "?" && tagValue != "") s"${tag.kifName}：$tagValue"
            else ""
          })
        }
      }
      .filter(_.nonEmpty)
      .mkString("\n")

  def renderSetup(fen: Option[FEN]): String = {
    fen.fold(s"${Tag.Handicap.kifName}：平手") { fen =>
      getHandicapName(fen).fold((Forsyth << fen.value).fold("")(renderSituation _))(hc =>
        s"${Tag.Handicap.kifName}：$hc"
      )
    }
  }

  def renderSituation(sit: Situation): String = {
    val kifBoard = new scala.collection.mutable.StringBuilder(256)
    val specialKifs: Map[Role, String] = Map( // one char versions
      PromotedSilver -> "全",
      PromotedKnight -> "圭",
      PromotedLance  -> "杏"
    )
    for (y <- 9 to 1 by -1) {
      kifBoard append "|"
      for (x <- 1 to 9) {
        sit.board(x, y) match {
          case None => kifBoard append " ・"
          case Some(piece) =>
            val color = if (piece.color == Gote) 'v' else ' '
            kifBoard append s"$color${specialKifs.getOrElse(piece.role, piece.role.kif)}"
        }
      }
      kifBoard append s"|${KifUtils.intToKanji(10 - y)}"
      if (y > 1) kifBoard append '\n'
    }
    List(
      sit.board.crazyData.fold("")(hs => "後手の持駒：" + renderHand(hs(Gote))),
      "  ９ ８ ７ ６ ５ ４ ３ ２ １",
      "+---------------------------+",
      kifBoard.toString,
      "+---------------------------+",
      sit.board.crazyData.fold("")(hs => "先手の持駒：" + renderHand(hs(Sente))),
      if (sit.color == Gote) "後手番" else ""
    ).filter(_.nonEmpty).mkString("\n")
  }

  private def renderHand(hand: Hand): String = {
    if (hand.size == 0) "なし"
    else
      Role.handRoles
        .map { r =>
          val cnt = hand(r)
          if (cnt == 1) r.kif
          else if (cnt > 1) r.kif + KifUtils.intToKanji(cnt)
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
    s"${((10 - sq.x) + 48 + 65248).toChar}${KifUtils.intToKanji(10 - sq.y)}"

  private def makeOrigSquare(sq: Pos): String =
    sq.usiKey.map(KifUtils toDigit _)

  private def getHandicapName(fen: FEN): Option[String] =
    StartingPosition.searchHandicapByFen(fen.some).map(t => t.eco)

  private def clockString(cur: NotationMove): Option[String] =
    cur.secondsSpent.map(spent =>
      s"${offset}(${formatKifSpent(spent)}/${cur.secondsTotal.fold("")(total => formatKifTotal(total))})"
    )

  private val offset = "   "

  private val noDoubleLineBreakRegex = "(\r?\n){2,}".r

  private def plyOffset(ply: Int) =
    f"$ply%4d"

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
