package shogi
package format

import cats.implicits._

import variant.{ Standard, Variant }

object Forsyth {

  val initial = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"

  def <<@(variant: Variant, rawSource: String): Option[Situation] =
    read(rawSource) { fen =>
      makeBoard(variant, fen) map { board =>
        val splitted    = fen split ' '
        val colorOption = splitted lift 1 flatMap (_ lift 0) flatMap Color.apply
        val situation = colorOption match {
          case Some(color)            => Situation(board, color)
          case _ if board.check(Gote) => Situation(board, Gote) // user in check will move first
          case _                      => Situation(board, Sente)
        }
        situation withHistory {
          History(lastMove = None, positionHashes = Array.empty)
        }
      }
    }

  def <<(rawSource: String): Option[Situation] = <<@(Standard, rawSource)

  case class SituationPlus(situation: Situation, moveNumber: Int) {

    def turns = moveNumber - (if ((moveNumber % 2 == 1) == situation.color.sente) 1 else 0)

  }

  def <<<@(variant: Variant, rawSource: String): Option[SituationPlus] =
    read(rawSource) { source =>
      <<@(variant, source) map { sit =>
        val splitted   = source.split(' ').drop(3)
        val moveNumber = splitted lift 0 flatMap (_.toIntOption) map (_ max 1 min 500)
        SituationPlus(
          sit,
          moveNumber getOrElse 1
        )
      }
    }

  def <<<(rawSource: String): Option[SituationPlus] = <<<@(Standard, rawSource)

  def makeBoard(variant: Variant, rawSource: String): Option[Board] =
    read(rawSource) { fen =>
      val splitted  = fen.split(' ')
      val positions = splitted.lift(0).getOrElse("")
      val ranks     = positions.count('/' ==)
      if (ranks == (variant.numberOfRanks - 1)) {
        makePiecesList(variant, positions.toList, "", variant.numberOfFiles, 1) map { case pieces =>
          val board = Board(pieces, variant)
          if (splitted.length < 3 || splitted.lift(2).get == "-") board
          else {
            val hands = readHands(variant, splitted.lift(2).get)
            board.withHandData(hands)
          }
        }
      } else None
    }

  def readHands(variant: Variant, sfenHand: String): Hands = {
    var curCnt = 0
    var total  = 1
    var sente  = Hand.init(variant)
    var gote   = Hand.init(variant)
    sfenHand foreach { p =>
      if ('0' <= p && p <= '9') {
        curCnt = curCnt * 10 + (p - '0').toInt
        total = curCnt
      } else {
        Role.allByForsyth.get(p.toLower.toString) map { role =>
          if (variant.handRoles.contains(role)) {
            val toStore = Math.min(total, 81)
            if (p.isUpper) sente = sente.store(role, toStore)
            else gote = gote.store(role, toStore)
          }
        }
        curCnt = 0
        total = 1
      }
    }
    Hands(sente, gote)
  }

  private def makePiecesList(
      variant: Variant,
      chars: List[Char],
      current: String,
      x: Int,
      y: Int
  ): Option[List[(Pos, Piece)]] =
    chars match {
      case Nil                               => Option(Nil)
      case '/' :: rest                       => makePiecesList(variant, rest, "", variant.numberOfFiles, y + 1)
      case '+' :: rest                       => makePiecesList(variant, rest, "+", x, y)
      case c :: rest if '1' <= c && c <= '9' => makePiecesList(variant, rest, "", x - (c - '0').toInt, y)
      case c :: rest =>
        for {
          pos          <- Pos.at(x, y)
          piece        <- Piece.fromForsyth(current + c)
          (nextPieces) <- makePiecesList(variant, rest, "", x - 1, y)
        } yield (pos -> piece :: nextPieces)
    }

  def >>(situation: Situation): String = >>(SituationPlus(situation, 1))

  def >>(parsed: SituationPlus): String =
    parsed match {
      case SituationPlus(situation, _) =>
        >>(
          Game(
            situation,
            turns = parsed.turns,
            startedAtTurn = parsed.turns,
            startedAtMove = parsed.moveNumber
          )
        )
    }

  def >>(game: Game): String =
    List(
      exportBoard(game.board),
      game.player.letter,
      exportHands(game.board),
      game.moveNumber
    ) mkString " "

  def exportSituation(situation: Situation): String =
    List(
      exportBoard(situation.board),
      situation.color.letter,
      exportHands(situation.board)
    ) mkString " "

  def exportHand(variant: Variant, hand: Hand): String =
    variant.handRoles map { r =>
      val cnt = hand(r)
      if (cnt == 1) r.forsyth
      else if (cnt > 1) cnt.toString + r.forsyth
      else ""
    } mkString ""

  def exportHands(board: Board): String =
    board.handData.fold("-") { hands =>
      val fullHandString =
        exportHand(board.variant, hands.sente).toUpperCase + exportHand(board.variant, hands.gote)
      if (fullHandString.isEmpty) "-"
      else fullHandString
    }

  def exportBoard(board: Board): String = {
    val fen   = new scala.collection.mutable.StringBuilder(256)
    var empty = 0
    for (y <- 1 to board.variant.numberOfRanks) {
      empty = 0
      for (x <- board.variant.numberOfFiles to 1 by -1) {
        board(x, y) match {
          case None => empty = empty + 1
          case Some(piece) =>
            if (empty == 0) fen append piece.forsyth
            else {
              fen append (empty.toString + piece.forsyth)
              empty = 0
            }
        }
      }
      if (empty > 0) fen append empty
      if (y < board.variant.numberOfRanks) fen append '/'
    }
    fen.toString
  }

  def getMoveNumber(rawSource: String): Option[Int] =
    read(rawSource) { fen =>
      fen.split(' ').lift(3).flatMap(_.toIntOption)
    }

  def getColor(rawSource: String): Option[Color] =
    read(rawSource) { fen =>
      fen.split(' ').lift(1) flatMap (_.headOption) flatMap Color.apply
    }

  def truncateFen(fen: String) = fen split ' ' take 3 mkString " "

  def compareTruncated(a: String, b: String) = truncateFen(a) == truncateFen(b)

  private def read[A](source: String)(f: String => A): A = f(source.replace("_", " ").trim)
}
