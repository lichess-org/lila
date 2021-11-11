package shogi
package format
package kif

import variant._

import cats.data.Validated
import cats.data.Validated.{ invalid, valid, Valid }
import cats.implicits._

object KifParserHelper {

  def parseSituation(
      str: String,
      handicap: Option[String]
  ): Validated[String, Situation] = {
    val lines = augmentString(str).linesIterator.to(List).map(_.trim.replace("：", ":").replace("　", " "))
    val ranks = lines
      .filter(l => (l lift 0 contains '|') && (l.length <= 42))
      .map(
        _.replace(".", "・")
          .replace(" ", "")
          .drop(1)
          .takeWhile(_ != '|')
      )

    val variant = detectVariant(ranks, handicap) | Standard

    if (ranks.size == 0) {
      handicap.filterNot(h => KifUtils.defaultHandicaps.exists(_._2 == h)).fold(valid(Situation(variant)): Validated[String, Situation]) { h =>
        parseHandicap(h)
      }
    } else if (ranks.size == variant.numberOfRanks) {
      for {
        pieces <- parseBoard(variant, ranks)
        senteHandStr = lines.find(l => l.startsWith("先手の持駒:") || l.startsWith("下手の持駒:")).getOrElse("")
        goteHandStr  = lines.find(l => l.startsWith("後手の持駒:") || l.startsWith("上手の持駒:")).getOrElse("")
        senteHand <- parseHand(variant, senteHandStr)
        goteHand  <- parseHand(variant, goteHandStr)
        hands    = Hands(senteHand, goteHand)
        board    = Board(pieces, variant).withCrazyData(hands)
        goteTurn = lines.exists(l => l.startsWith("後手番") || l.startsWith("上手番"))
      } yield (Situation(board, Color.fromSente(!goteTurn)))
    } else {
      invalid(
        s"Cannot parse board setup (not enough ranks provided ${ranks.size}/${variant.numberOfRanks})"
      )
    }
  }

  private def detectVariant(ranks: List[String], handicap: Option[String]): Option[Variant] = {
    if (
      ranks.size == 5 ||
      handicap.exists(h => KifUtils.defaultHandicaps.get(Minishogi).fold(false)(_ == h))
    ) Minishogi.some
    else None
  }

  private def parseBoard(variant: Variant, ranks: List[String]): Validated[String, List[(Pos, Piece)]] = {
    def makePiecesList(
        chars: List[Char],
        x: Int,
        y: Int,
        prevPieceChar: Option[Char],
        gote: Boolean
    ): Validated[String, List[(Pos, Piece)]] =
      chars match {
        case Nil                 => valid(Nil)
        case '・' :: rest         => makePiecesList(rest, x - 1, y, None, false)
        case ('v' | 'V') :: rest => makePiecesList(rest, x, y, None, true)
        case ('成' | '+') :: rest => makePiecesList(rest, x, y, chars.headOption, gote)
        case sq :: rest =>
          for {
            pos <- Pos.at(x, y) toValid s"Too many files in board setup on rank $y"
            roleStr = prevPieceChar.fold("")(_.toString) + sq
            role   <- Role.allByEverything.get(roleStr) toValid s"Unknown piece in board setup: $roleStr"
            _      <- if(variant.allRoles contains role) valid(role)
                      else invalid(s"$role is not valid $variant variant")
            pieces <- makePiecesList(rest, x - 1, y, None, false)
          } yield (pos -> Piece(Color.fromSente(!gote), role) :: pieces)
      }
    ranks.zipWithIndex.foldLeft(valid(Nil): Validated[String, List[(Pos, Piece)]]) { case (acc, cur) =>
      for {
        pieces     <- acc
        nextPieces <- makePiecesList(cur._1.toList, variant.numberOfFiles, cur._2 + 1, None, false)
      } yield (pieces ::: nextPieces)
    }
  }

  private def parseHand(variant: Variant, str: String): Validated[String, Hand] = {
    def parseHandPiece(str: String, hand: Hand): Validated[String, Hand] =
      for {
        roleStr <- str.headOption toValid "Cannot parse hand"
        num = KifUtils kanjiToInt str.tail
        role <- Role.allByEverything.get(roleStr.toString) toValid s"Unknown piece in hand: $roleStr"
        _ <-
          if (variant.handRoles contains role) valid(role)
          else invalid(s"Cannot place $role in hand in $variant variant")
      } yield (hand.store(role, num))
    val values = str.split(":").lastOption.getOrElse("").trim
    if (values == "なし" || values == "") valid(Hand.init(variant))
    else {
      values.split(" ").foldLeft(valid(Hand.init(variant)): Validated[String, Hand]) { case (acc, cur) =>
        acc match {
          case Valid(hand) => parseHandPiece(cur, hand)
          case _           => acc
        }
      }
    }
  }

  private def parseHandicap(str: String): Validated[String, Situation] =
    for {
      hPosition <- StartingPosition.searchByEco(str) toValid s"Unknown handicap: $str"
      situation <- Forsyth << hPosition.fen toValid s"Cannot parse handicap: $str"
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
