package shogi
package format
package kif

import variant.Variant

import scalaz.Validation.FlatMap._
import scalaz.Validation.{ success => succezz }

object KifParserHelper {

  def parseSituation(str: String, handicap: Option[String], variant: Variant): Valid[Situation] = {
    val lines = augmentString(str).linesIterator.to(List).map(_.trim.replace("：", ":").replace("　", " "))
    val ranks = lines
      .filter(l => (l lift 0 contains '|') && (l.length <= 42))
      .map(
        _.replace(".", "・")
          .replace(" ", "")
          .drop(1)
          .takeWhile(_ != '|')
      )

    if (ranks.size == 0) {
      handicap.filterNot(_ == "平手").fold(succezz(Situation(variant)): Valid[Situation]) { h =>
        parseHandicap(h, variant)
      }
    } else if (ranks.size == 9) {
      for {
        pieces <- parseBoard(ranks)
        senteHandStr = lines.find(l => l.startsWith("先手の持駒:") || l.startsWith("下手の持駒:")).getOrElse("")
        goteHandStr  = lines.find(l => l.startsWith("後手の持駒:") || l.startsWith("上手の持駒:")).getOrElse("")
        senteHand <- parseHand(senteHandStr)
        goteHand  <- parseHand(goteHandStr)
        hands    = Hands(senteHand, goteHand)
        board    = Board(pieces, variant).withCrazyData(hands)
        goteTurn = lines.exists(l => l.startsWith("後手番") || l.startsWith("上手番"))
      } yield (Situation(board, Color(!goteTurn)))
    } else {
      "Cannot parse board setup starting with this line %s (not enough ranks provided %d/9)"
        .format(ranks.head, ranks.size)
        .failureNel
    }
  }

  private def parseBoard(ranks: List[String]): Valid[List[(Pos, Piece)]] = {
    def makePiecesList(
        chars: List[Char],
        x: Int,
        y: Int,
        prevPieceChar: Option[Char],
        gote: Boolean
    ): Valid[List[(Pos, Piece)]] =
      chars match {
        case Nil                 => succezz(Nil)
        case '・' :: rest         => makePiecesList(rest, x + 1, y, None, false)
        case ('v' | 'V') :: rest => makePiecesList(rest, x, y, None, true)
        case ('成' | '+') :: rest => makePiecesList(rest, x, y, chars.headOption, gote)
        case sq :: rest =>
          for {
            pos <- Pos.posAt(x, y) toValid s"Too many files in board setup: $x/9"
            roleStr = prevPieceChar.fold("")(_.toString) + sq
            role   <- Role.allByEverything.get(roleStr) toValid s"Unknown piece in board setup: $roleStr"
            pieces <- makePiecesList(rest, x + 1, y, None, false)
          } yield (pos -> Piece(Color(!gote), role) :: pieces)
      }
    ranks.zipWithIndex.foldLeft(succezz(Nil): Valid[List[(Pos, Piece)]]) { case (acc, cur) =>
      for {
        pieces     <- acc
        nextPieces <- makePiecesList(cur._1.toList, 1, 9 - cur._2, None, false)
      } yield (pieces ::: nextPieces)
    }
  }

  private def parseHand(str: String): Valid[Hand] = {
    def parseHandPiece(str: String, hand: Hand): Valid[Hand] =
      for {
        roleStr <- str.headOption toValid "Cannot parse hand"
        num = KifUtils kanjiToInt str.tail
        role <- Role.allByEverything.get(roleStr.toString) toValid s"Unknown piece in hand: $roleStr"
      } yield (hand.store(role, num))
    val values = str.split(":").lastOption.getOrElse("").trim
    if (values == "なし" || values == "") succezz(Hand.init)
    else {
      values.split(" ").foldLeft(succezz(Hand.init): Valid[Hand]) { case (acc, cur) =>
        acc match {
          case scalaz.Success(hand) => parseHandPiece(cur, hand)
          case _                    => acc
        }
      }
    }
  }

  private def parseHandicap(str: String, variant: Variant): Valid[Situation] =
    for {
      hPosition <- StartingPosition.searchByEco(str) toValid s"Unknown handicap: $str"
      situation <- Forsyth.<<@(variant, hPosition.fen) toValid s"Cannot parse handicap: $str"
    } yield situation

  def createResult(termination: Option[Tag], color: Color): Option[Tag] = {
    termination.map(_.value.toLowerCase) match {
      case Some("投了") | Some("反則負け") | Some("切れ負け") | Some("time-up") =>
        Tag(_.Result, color.fold("0-1", "1-0")).some
      case Some("入玉勝ち") | Some("詰み") | Some("反則勝ち") => Tag(_.Result, color.fold("1-0", "0-1")).some
      case Some("持将棋") | Some("千日手")                => Tag(_.Result, "1/2-1/2").some
      case _                                        => None
    }
  }

}
