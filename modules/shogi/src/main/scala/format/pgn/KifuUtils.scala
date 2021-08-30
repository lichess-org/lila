package shogi
package format
package pgn

import scala._

object KifUtils {
  val tagParse = Map[TagType, String](
    Tag.Event       -> "棋戦",
    Tag.Site        -> "場所",
    Tag.TimeControl -> "持ち時間",
    Tag.Handicap    -> "手合割",
    Tag.Sente       -> "先手",
    Tag.Gote        -> "後手",
    Tag.Opening     -> "戦型"
  )

  def toDigit(c: Char): Char = {
    c match {
      case '１' | '一' | 'a' =>
        return '1'
      case '２' | '二' | 'b' =>
        return '2'
      case '３' | '三' | 'c' =>
        return '3'
      case '４' | '四' | 'd' =>
        return '4'
      case '５' | '五' | 'e' =>
        return '5'
      case '６' | '六' | 'f' =>
        return '6'
      case '７' | '七' | 'g' =>
        return '7'
      case '８' | '八' | 'h' =>
        return '8'
      case '９' | '九' | 'i' =>
        return '9'
      case _ =>
        return c
    }
  }

  def toKanjiDigit(c: Char): Char = {
    c match {
      case '1' =>
        return '一'
      case '2' =>
        return '二'
      case '3' =>
        return '三'
      case '4' =>
        return '四'
      case '5' =>
        return '五'
      case '6' =>
        return '六'
      case '7' =>
        return '七'
      case '8' =>
        return '八'
      case '9' =>
        return '九'
      case _ =>
        return c
    }
  }

  // supporting max 999
  def kanjiToInt(str: String): Int = {
    def orderHelper(ord: String): Int = {
      if (ord == "") 1
      else if (ord.contains('十')) 10 * orderHelper(ord.filterNot(_ == '十'))
      else if (ord.contains('百')) 100 * orderHelper(ord.filterNot(_ == '百'))
      else parseIntOption(ord.map(toDigit _)).getOrElse(0)
    }
    str.split("""(?<=(百|十))""").foldLeft(0) { (acc, cur) =>
      acc + orderHelper(cur)
    }
  }

  // supporting max 999
  def intToKanji(num: Int): String = {
    if(num >= 200) intToKanji(num / 100) + "百" + intToKanji(num % 100)
    else if(num >= 100) "百" + intToKanji(num % 100)
    else if(num >= 20) intToKanji(num / 10) + "十" + intToKanji(num % 10)
    else if(num >= 10) "十" + intToKanji(num % 10)
    else num.toString.map(toKanjiDigit _)
  }

  // 後手の持駒:金四 銀二 香三 歩十三
  def readHands(str: String = ""): String = {
    val values = str.split(":").lastOption.getOrElse("").trim
    if (values == "なし" || values == "") return ""
    var hand = Hand.init
    for (p <- values.split(" ")) {
      val role = p.headOption
      val num  = p.tail
      Role.allByEverything.get(role.fold("")(_.toString)) map { r =>
        hand = hand.store(r, KifUtils kanjiToInt num)
      }
    }
    hand.exportHand
  }

  def handicapToFen(str: String): Option[Tag] = {
    StartingPosition.searchByEco(str) map { h =>
      Tag("FEN", h.fen)
    }
  }

  def readBoard(str: String): Option[Tag] = {
    val lines = augmentString(str).linesIterator.to(List).map(_.trim)
    val ranks = lines.map(_.trim).filter(l => (l lift 0 contains '|') && (l.length <= 42))
    val senteHandStr =
      lines.map(_.trim).filter(l => l.startsWith("先手の持駒:") || l.startsWith("下手の持駒:"))
    val goteHandStr =
      lines.map(_.trim).filter(l => l.startsWith("後手の持駒:") || l.startsWith("上手の持駒:"))
    val goteBan = lines.map(_.trim).exists(l => l.startsWith("後手番") || l.startsWith("上手番"))

    if (ranks.length != 9) return None

    var rank = 0
    val sfen = new scala.collection.mutable.StringBuilder(256)
    for (r <- ranks) {
      r.replace(".", "・")
      var empty    = 0
      var gote     = false
      var pieceStr = ""
      for (sq <- r) {
        sq match {
          case '・' => {
            empty = empty + 1
          }
          case 'v' | 'V' => {
            gote = true
          }
          case '成' | '+' =>
            pieceStr = sq.toString
          case _ => {
            Role.allByEverything.get(pieceStr + sq) map { r =>
              if (empty > 0) sfen append empty.toString
              sfen append Piece(Color(!gote), r).forsythFull
              empty = 0
            }
            pieceStr = ""
            gote = false
          }
        }
      }
      rank += 1
      if (empty > 0) sfen append empty.toString
      if (rank != 9) sfen append '/'
    }
    if (goteBan) sfen.append(" w ") else sfen.append(" b ")
    val senteHand = readHands(senteHandStr.headOption.getOrElse("")).toUpperCase
    val goteHand  = readHands(goteHandStr.headOption.getOrElse(""))

    if (senteHand == "" && senteHandStr == goteHand) sfen.append("-")
    else sfen.append(senteHand + goteHand)

    Tag(_.FEN, sfen).some
  }

  def writeKifSituation(fen: FEN): String = {
    val kifBoard   = new scala.collection.mutable.StringBuilder(256)
    val specialKifs: Map[Role, String] = Map( // one char versions
      PromotedSilver -> "全",
      PromotedKnight -> "圭",
      PromotedLance -> "杏"
    )
    Forsyth << fen.value match {
      case Some(sit) => {
        for (y <- 9 to 1 by -1) {
          kifBoard append "|"
          for (x <- 1 to 9) {
            sit.board(x, y) match {
              case None => kifBoard append " ・"
              case Some(piece) =>
                val color = if(piece.color == Gote) 'v' else ' '
                kifBoard append s"$color${specialKifs.getOrElse(piece.role, piece.role.kif)}"
            }
          }
          kifBoard append s"|${intToKanji(10 - y)}"
          if (y > 1) kifBoard append '\n'
        }
        List(
          sit.board.crazyData.fold("")(hs => "後手の持駒：" + writeHand(hs(Gote)).trim),
          "+---------------------------+",
          kifBoard.toString,
          "+---------------------------+",
          sit.board.crazyData.fold("")(hs => "先手の持駒：" + writeHand(hs(Sente)).trim),
          if (sit.color == Gote) "後手番" else ""
        ).filter(_.nonEmpty).mkString("\n")
      }
      case _ => s"SFEN: $fen"
    }
  }

  def writeHand(hand: Hand): String = {
    if(hand.size == 0) "なし"
    else Role.handRoles.map { r =>
      val cnt = hand(r)
      if (cnt == 1) r.kif
      else if (cnt > 1) r.kif + intToKanji(cnt)
      else ""
    } mkString " "
  }

  def getHandicapName(fen: FEN): Option[String] =
    StartingPosition.searchHandicapByFen(fen.some).map(t => t.eco)
  
  def createResult(tags: Tags, color: Color): Option[Tag] = {
    tags(_.Termination).map(_.toLowerCase) match {
      case Some("投了") | Some("反則負け") | Some("切れ負け") | Some("Time-up") =>
        Tag(_.Result, color.fold("0-1", "1-0")).some
      case Some("入玉勝ち") | Some("詰み") | Some("反則勝ち") => Tag(_.Result, color.fold("1-0", "0-1")).some
      case Some("持将棋") | Some("千日手")                => Tag(_.Result, "1/2-1/2").some
      case _                                        => None
    }
  }

  // in this order
  val kifTags = List(
    Tag.Event,
    Tag.Site,
    Tag.TimeControl,
    Tag.Handicap,
    Tag.Sente,
    Tag.Gote,
    Tag.Opening
  )

  def kifHeader(tags: Tags): String =
    kifTags.map { kt => 
      tagParse.get(kt).fold("") { kifTagName =>
        // we need these even empty
        if (kt == Tag.Sente || kt == Tag.Gote) {
          val playername = tags(kt.name).getOrElse("")
          s"${kifTagName}：$playername"
        }
        else if (kt == Tag.Handicap) {
          tags.fen.fold(s"${kifTagName}：平手") { fen =>
            getHandicapName(fen).fold(writeKifSituation(fen))(hc => s"${kifTagName}：$hc")
          }
        }
        else {
          tags(kt.name).fold("")(tagValue => {
            if(tagValue != "?") s"${kifTagName}：$tagValue"
            else ""
          })
        }
    }
  }.filter(_.nonEmpty).mkString("\n")

  def makeDestSquare(sq: Pos): String = 
    s"${((10 - sq.x) + 48 + 65248).toChar}${intToKanji(10 - sq.y)}"

  def makeOrigSquare(sq: Pos): String =
    sq.usiKey.map(toDigit _)

  def moveKif(uci: String, san: String, lastDest: Option[Pos]): String =
    Uci(uci) match {
      case Some(Uci.Drop(role, pos)) =>
        s"${makeDestSquare(pos)}${role.kif}打"
      case Some(Uci.Move(orig, dest, prom)) => {
        val destStr = if (lastDest.fold(false)(_ == dest)) "同　" else makeDestSquare(dest)
        val promStr = if (prom) "成" else ""
        san.headOption.flatMap(s => Role.allByPgn.get(s)).fold(s"move parse error - $uci, $san"){ r =>
          s"$destStr${r.kif}$promStr(${makeOrigSquare(orig)})"
        }
      }
      case _ => s"parse error - $uci, $san"
    }

  // Use kifmodel instead
  def movesAsKifu(uciPgn: Vector[(String, String)]): Vector[String] = {
    uciPgn.foldLeft((Vector[String](), None: Option[Pos])) { case ((acc, lastDest), cur) =>
      // t is a tuple of (uci, pgn)
      val kifMove = cur match {
        case (uci, san) => moveKif(uci, san, lastDest)
      }
      val prevDest = cur match {
        case (uci, _) => Uci(uci).map(_.origDest._2)
      }
      (acc :+ kifMove, prevDest)
    }._1
  }
}
//Result & Termination <-> saigo no te
