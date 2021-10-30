package shogi
package format
package csa

import variant.Standard

import cats.data.Validated
import cats.data.Validated.{ invalid, valid, Valid }
import cats.implicits._

object CsaParserHelper {
  def parseSituation(str: String): Validated[String, Situation] = {
    val lines        = augmentString(str.replace(",", "\n")).linesIterator.to(List).map(_.trim)
    val handicap     = lines.find(l => l.startsWith("PI"))
    val isBoardSetup = lines.exists(l => l.startsWith("P1"))
    if (handicap.isDefined && isBoardSetup)
      invalid("Both handicap (PI) and whole board position (P1, P2, ...) provided")
    else if (!handicap.isDefined && !isBoardSetup)
      invalid("No initial position provided (add 'PI' for standard position)")
    else {
      val initPieces = handicap.fold {
        val ranks = lines.filter(l => l.startsWith("P") && l.lift(1).exists(_.isDigit))
        parseWholeBoard(ranks)
      }(h => parseHandicap(h))
      for {
        pieces <- initPieces
        board     = Board(pieces, Standard)
        goteTurn  = lines.exists(l => l == "-")
        additions = lines.filter(l => l.startsWith("P") && l.lift(1).exists(List('+', '-') contains _))
        board2 <- parseAdditions(additions, board)
      } yield (Situation(board2, Color.fromSente(!goteTurn)))
    }
  }

  private def parseHandicap(handicap: String): Validated[String, PieceMap] = {
    def parseSquarePiece(squarePiece: String, pieces: PieceMap): Validated[String, PieceMap] = {
      for {
        _ <-
          if (squarePiece.size == 4) valid(squarePiece)
          else invalid(s"Incorrect square and piece format in handicap setup: $squarePiece")
        posStr  = squarePiece.slice(0, 2)
        roleStr = squarePiece.slice(2, 4)
        pos <- Pos.allNumberKeys get posStr toValid s"Incorrect position in handicap setup: $posStr"
        _ <-
          if (pieces contains pos) valid(pos)
          else invalid(s"No piece to remove from $posStr in handicap setup")
        role <- Role.allByCsa get roleStr toValid s"Non existent piece role in handicap setup: $roleStr"
        _ <-
          if (pieces.get(pos).exists(_.role == role)) valid(role)
          else invalid(s"$role not present on $posStr in handicap setup")
      } yield (pieces - pos)
    }

    handicap.drop(2).grouped(4).foldLeft(valid(Standard.pieces): Validated[String, PieceMap]) {
      case (acc, cur) =>
        acc match {
          case Valid(pieces) => parseSquarePiece(cur, pieces)
          case _             => acc
        }
    }
  }

  private def parseWholeBoard(ranks: List[String]): Validated[String, PieceMap] = {
    def parseSquare(sq: String, i: Int, pieces: PieceMap): Validated[String, PieceMap] =
      for {
        pos <- Pos.at(
          9 - i % 9,
          (i / 9 + 1)
        ) toValid s"Invalid board setup - too many squares"
        piece <- Piece.fromCsa(sq) toValid s"Non existent piece (${sq}) in board setup"
      } yield (pieces + (pos -> piece))
    val squares = ranks.flatMap(_.drop(2).grouped(3)).zipWithIndex
    if (ranks.size != 9) invalid("Incorrect number of board ranks in board setup: %d/9".format(ranks.size))
    else if (squares.size != 81)
      invalid(
        "Incorrect number of squares in board setup: %d/81 (%s)"
          .format(squares.size, ranks.filter(_.size > (2 + 9 * 3)).map(_.take(2)).mkString(","))
      )
    else {
      squares.foldLeft(valid(Map()): Validated[String, PieceMap]) { case (acc, cur) =>
        acc match {
          case Valid(pieces) =>
            if (cur._1.contains("*")) acc
            else parseSquare(cur._1, cur._2, pieces)
          case _ => acc
        }
      }
    }
  }

  private def parseAdditions(
      additions: List[String],
      board: Board
  ): Validated[String, Board] = {
    def parseSingleLine(line: String, board: Board): Validated[String, Board] = {
      def parseHandAddition(
          str: String,
          color: Color,
          board: Board
      ): Validated[String, Board] = {
        if (str == "00AL") {
          val hands        = board.crazyData.getOrElse(Hands.init(Standard))
          val otherHand    = hands(!color)
          val initialRoles = Standard.pieces.values.toList
          val curBoard     = board.pieces.values.toList
          val newHand = Standard.handRoles.foldLeft(Hand.init(Standard)) { case (acc, cur) =>
            val n = initialRoles.count(_.role == cur) - otherHand(cur) - curBoard.count(_.role == cur)
            acc.store(cur, Math.max(n, 0))
          }
          val newHands = color.fold(
            hands.copy(sente = newHand),
            hands.copy(gote = newHand)
          )
          valid(board withCrazyData newHands)
        } else
          for {
            _ <-
              if (str.size == 4) valid(str)
              else invalid(s"Incorrect format (${str}) in: $line")
            roleStr = str.slice(2, 4)
            role <- Role.allByCsa get roleStr toValid s"Non existent piece role (${roleStr}) in: $line"
            _ <-
              if (Standard.handRoles.contains(role)) valid(role)
              else invalid(s"Can't have $role in hand: $line")
            hands = board.crazyData.getOrElse(Hands.init(Standard))
          } yield (board.withCrazyData(hands.store(Piece(!color, role))))
      }
      def parseBoardAddition(str: String, color: Color, board: Board): Validated[String, Board] = {
        for {
          _ <-
            if (str.size == 4) valid(str)
            else invalid(s"Incorrect square piece format (${str}) in: $line")
          posStr  = str.slice(0, 2)
          roleStr = str.slice(2, 4)
          pos  <- Pos.allNumberKeys get posStr toValid s"Incorrect position (${posStr}) in: $line"
          role <- Role.allByCsa get roleStr toValid s"Non existent piece role (${roleStr}) in: $line"
          boardWithPiece <- board
            .place(
              Piece(color, role),
              pos
            ) toValid s"Cannot place $role on $posStr - already occupied - in: $line"
        } yield boardWithPiece
      }

      val color = Color.fromSente(line.lift(1).exists(_ == '+'))
      line.drop(2).grouped(4).foldLeft(valid(board): Validated[String, Board]) { case (acc, cur) =>
        acc match {
          case Valid(board) =>
            if (cur startsWith "00")
              parseHandAddition(cur, color, board)
            else
              parseBoardAddition(cur, color, board)
          case _ => acc
        }
      }
    }

    if (
      additions.exists(a => a contains "00AL") &&
      additions.lastOption.fold(false)(!_.endsWith("00AL"))
    )
      invalid("00AL must be the last addition made")
    else {
      additions.foldLeft(valid(board): Validated[String, Board]) { case (acc, cur) =>
        acc match {
          case Valid(board) => parseSingleLine(cur, board)
          case _            => acc
        }
      }
    }

  }

  def createResult(termination: Option[Tag], color: Color): Option[Tag] =
    termination.map(_.value.toUpperCase) match {
      case Some("TORYO") | Some("TIME_UP") | Some("ILLEGAL_MOVE") =>
        Tag(_.Result, color.fold("0-1", "1-0")).some
      case Some("+ILLEGAL_ACTION")                                 => Tag(_.Result, "1-0").some
      case Some("-ILLEGAL_ACTION")                                 => Tag(_.Result, "0-1").some
      case Some("KACHI") | Some("TSUMI")                           => Tag(_.Result, color.fold("1-0", "0-1")).some
      case Some("JISHOGI") | Some("SENNICHITE") | Some("HIKIWAKE") => Tag(_.Result, "1/2-1/2").some
      case _                                                       => None
    }

}
