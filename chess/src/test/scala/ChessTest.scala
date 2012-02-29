package lila.chess

import ornicar.scalalib.test.OrnicarValidationMatchers
import org.specs2.mutable.Specification
import org.specs2.matcher.{ Matcher }

import format.Visual

trait ChessTest
    extends Specification
    with OrnicarValidationMatchers {

  implicit def stringToBoard(str: String): Board = Visual << str

  implicit def stringToSituationBuilder(str: String) = new {

    def as(color: Color): Situation = Situation(Visual << str, color)
  }

  implicit def richGame(game: Game) = new {

    def as(color: Color): Game = game.copy(player = color)

    def playMoves(moves: (Pos, Pos)*): Valid[Game] =
      moves.foldLeft(success(game): Valid[Game]) { (vg, move) ⇒
        vg flatMap { g ⇒ g.playMove(move._1, move._2) }
      }
  }

  def bePoss(poss: Pos*): Matcher[Option[Iterable[Pos]]] = beSome.like {
    case p ⇒ sortPoss(p.toList) must_== sortPoss(poss.toList)
  }

  def bePoss(board: Board, visual: String): Matcher[Option[Iterable[Pos]]] = beSome.like {
    case p ⇒ Visual.addNewLines(Visual.>>|(board, Map(p -> 'x'))) must_== visual
  }

  def beBoard(visual: String): Matcher[Valid[Board]] = beSuccess.like {
    case b ⇒ b.visual must_== (Visual << visual).visual
  }

  def beSituation(visual: String): Matcher[Valid[Situation]] = beSuccess.like {
    case s ⇒ s.board.visual must_== (Visual << visual).visual
  }

  def beGame(visual: String): Matcher[Valid[Game]] = beSuccess.like {
    case g ⇒ g.board.visual must_== (Visual << visual).visual
  }

  def sortPoss(poss: Seq[Pos]): Seq[Pos] = poss sortBy (_.toString)

  def pieceMoves(piece: Piece, pos: Pos): Option[List[Pos]] =
    (Board.empty place piece at pos).toOption flatMap { b ⇒
      b actorAt pos map (_.destinations)
    }
}
