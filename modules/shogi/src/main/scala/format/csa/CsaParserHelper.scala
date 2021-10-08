package shogi
package format
package csa

import variant.Variant

import scalaz.Validation.FlatMap._
import scalaz.Validation.{ success => succezz }

object CsaParserHelper {
  def parseSituation(str: String, variant: Variant): Valid[Situation] = {
    val lines        = augmentString(str.replace(",", "\n")).linesIterator.to(List).map(_.trim)
    val handicap     = lines.find(l => l.startsWith("PI"))
    val isBoardSetup = lines.exists(l => l.startsWith("P1"))
    if (handicap.isDefined && isBoardSetup)
      "Both handicap (PI) and whole board position (P1, P2, ...) provided".failureNel
    else if (!handicap.isDefined && !isBoardSetup)
      "No initial position provided (add 'PI' for standard position)".failureNel
    else {
      val initPieces = handicap.fold {
        val ranks = lines.filter(l => l.startsWith("P") && l.lift(1).exists(_.isDigit))
        parseWholeBoard(ranks)
      }(h => parseHandicap(h, variant))
      for {
        pieces <- initPieces
        board     = Board(pieces, variant)
        goteTurn  = lines.exists(l => l == "-")
        additions = lines.filter(l => l.startsWith("P") && l.lift(1).exists(List('+', '-') contains _))
        board2 <- parseAdditions(additions, board, variant)
      } yield (Situation(board2, Color(!goteTurn)))
    }
  }

  private def parseHandicap(handicap: String, variant: Variant): Valid[PieceMap] = {
    def parseSquarePiece(squarePiece: String, pieces: PieceMap): Valid[PieceMap] = {
      for {
        _ <- squarePiece.validIf(
          squarePiece.size == 4,
          s"Incorrect square and piece format in handicap setup: $squarePiece"
        )
        posStr  = squarePiece.slice(0, 2)
        roleStr = squarePiece.slice(2, 4)
        pos  <- Pos.numberAllKeys get posStr toValid s"Incorrect position in handicap setup: $posStr"
        _    <- pos.validIf(pieces contains pos, s"No piece to remove from $posStr in handicap setup")
        role <- Role.allByCsa get roleStr toValid s"Non existent piece role in handicap setup: $roleStr"
        _ <- role.validIf(
          pieces.get(pos).exists(_.role == role),
          s"$role not present on $posStr in handicap setup"
        )
      } yield (pieces - pos)
    }

    handicap.drop(2).grouped(4).foldLeft(succezz(variant.pieces): Valid[PieceMap]) { case (acc, cur) =>
      acc match {
        case scalaz.Success(pieces) => parseSquarePiece(cur, pieces)
        case _                      => acc
      }
    }
  }

  private def parseWholeBoard(ranks: List[String]): Valid[PieceMap] = {
    def parseSquare(sq: String, i: Int, pieces: PieceMap): Valid[PieceMap] =
      for {
        pos <- Pos.posAt(
          i % 9 + 1,
          10 - (i / 9 + 1)
        ) toValid s"Invalid board setup - too many squares"
        piece <- Piece.fromCsa(sq) toValid s"Non existent piece (${sq}) in board setup"
      } yield (pieces + (pos -> piece))
    val squares = ranks.flatMap(_.drop(2).grouped(3)).zipWithIndex
    if (ranks.size != 9) "Incorrect number of board ranks in board setup: %d/9".format(ranks.size).failureNel
    else if (squares.size != 81)
      "Incorrect number of squares in board setup: %d/81 (%s)"
        .format(squares.size, ranks.filter(_.size > (2 + 9 * 3)).map(_.take(2)).mkString(","))
        .failureNel
    else {
      squares.foldLeft(succezz(Map()): Valid[PieceMap]) { case (acc, cur) =>
        acc match {
          case scalaz.Success(pieces) =>
            if (cur._1.contains("*")) acc
            else parseSquare(cur._1, cur._2, pieces)
          case _ => acc
        }
      }
    }
  }

  private def parseAdditions(additions: List[String], board: Board, variant: Variant): Valid[Board] = {
    def parseSingleLine(line: String, board: Board, variant: Variant): Valid[Board] = {
      def parseHandAddition(str: String, color: Color, board: Board, variant: Variant): Valid[Board] = {
        if (str == "00AL") {
          val hands        = board.crazyData.getOrElse(Hands.init)
          val otherHand    = hands(!color)
          val initialRoles = variant.pieces.values.toList
          val curBoard     = board.pieces.values.toList
          val newHand = Role.handRoles.foldLeft(Hand.init) { case (acc, cur) =>
            val n = initialRoles.count(_.role == cur) - otherHand(cur) - curBoard.count(_.role == cur)
            acc.store(cur, Math.max(n, 0))
          }
          val newHands = color.fold(
            hands.copy(sente = newHand),
            hands.copy(gote = newHand)
          )
          succezz(board withCrazyData newHands)
        } else
          for {
            _ <- str.validIf(str.size == 4, s"Incorrect format (${str}) in: $line")
            roleStr = str.slice(2, 4)
            role <- Role.allByCsa get roleStr toValid s"Non existent piece role (${roleStr}) in: $line"
            _    <- role.validIf(Role.handRoles.contains(role), s"Can't have $role in hand: $line")
            hands = board.crazyData.getOrElse(Hands.init)
          } yield (board.withCrazyData(hands.store(Piece(!color, role))))
      }
      def parseBoardAddition(str: String, color: Color, board: Board): Valid[Board] = {
        for {
          _ <- str.validIf(str.size == 4, s"Incorrect square piece format (${str}) in: $line")
          posStr  = str.slice(0, 2)
          roleStr = str.slice(2, 4)
          pos  <- Pos.numberAllKeys get posStr toValid s"Incorrect position (${posStr}) in: $line"
          role <- Role.allByCsa get roleStr toValid s"Non existent piece role (${roleStr}) in: $line"
          boardWithPiece <- board
            .place(
              Piece(color, role),
              pos
            ) toValid s"Cannot place $role on $posStr - already occupied - in: $line"
        } yield boardWithPiece
      }

      val color = Color(line.lift(1).exists(_ == '+'))
      line.drop(2).grouped(4).foldLeft(succezz(board): Valid[Board]) { case (acc, cur) =>
        acc match {
          case scalaz.Success(board) =>
            if (cur startsWith "00")
              parseHandAddition(cur, color, board, variant)
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
      "00AL must be the last addition made".failureNel
    else {
      additions.foldLeft(succezz(board): Valid[Board]) { case (acc, cur) =>
        acc match {
          case scalaz.Success(board) => parseSingleLine(cur, board, variant)
          case _                     => acc
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
