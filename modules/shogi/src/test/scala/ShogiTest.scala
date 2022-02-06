package shogi

import cats.data.Validated
import cats.syntax.option._
import org.specs2.matcher.Matcher
import org.specs2.matcher.ValidatedMatchers
import org.specs2.mutable.Specification

import scala.annotation.nowarn

import format.forsyth.{ Sfen, Visual }
import format.usi.Usi
import variant._

trait ShogiTest extends Specification with ValidatedMatchers {

  implicit def stringToSituation(str: String): Situation = (Visual parse str).get

  implicit def colorChanger(str: String) =
    new {

      def as(color: Color): Situation = ((Visual parse str).get).copy(color = color)
    }

  case class RichActor(actor: MoveActor) {
    def threatens(to: Pos): Boolean =
      actor.piece.eyes(actor.pos, to) && {
        (actor.piece.projectionDirs.isEmpty) ||
        (actor.piece.directDirs.exists(_(actor.pos).contains(to))) ||
        actor.piece.role.dir(actor.pos, to).exists {
          actor.situation.variant.longRangeThreatens(actor.situation.board, actor.pos, _, to)
        }
      }
  }

  implicit def richActor(actor: MoveActor) = RichActor(actor)

  case class RichGame(game: Game) {

    def as(color: Color): Game = game.withColor(color)

    def playMoves(moves: (Pos, Pos, Boolean)*): Validated[String, Game] = playMoveList(moves)

    @nowarn def playMoveList(moves: Seq[(Pos, Pos, Boolean)]): Validated[String, Game] = {
      val vg = moves.foldLeft[Validated[String, Game]](Validated.valid(game)) {
        case (vg, (orig, dest, prom)) =>
          vg.foreach { _.situation.moveDestinations }
          val ng = vg flatMap { g =>
            g(Usi.Move(orig, dest, prom))
          }
          ng
      }
      vg
    }

    def playMove(
        orig: Pos,
        dest: Pos,
        promotion: Boolean = false
    ): Validated[String, Game] =
      game.apply(Usi.Move(orig, dest, promotion))

    def playDrop(
        role: Role,
        dest: Pos
    ): Validated[String, Game] =
      game.apply(Usi.Drop(role, dest))

    def withClock(c: Clock) = game.copy(clock = Some(c))
  }

  implicit def richGame(game: Game) = RichGame(game)

  def sfenToGame(sfen: Sfen, variant: Variant = shogi.variant.Standard) =
    sfen.toSituation(variant) toValid "Could not construct situation from SFEN" map { sit =>
      Game(variant).copy(
        situation = sit
      )
    }

  def makeSituation(pieces: (Pos, Piece)*): Situation =
    Situation(shogi.variant.Standard).withBoard(Board(pieces))

  def makeSituation: Situation = Situation(shogi.variant.Standard)

  def makeEmptySituation: Situation = Situation(shogi.variant.Standard).withBoard(Board.empty)

  def bePoss(poss: Pos*): Matcher[Option[Iterable[Pos]]] =
    beSome.like { case p =>
      sortPoss(p.toList) must_== sortPoss(poss.toList)
    }

  def makeGame: Game = Game(makeSituation)

  def bePoss(situation: Situation, visual: String): Matcher[Option[Iterable[Pos]]] =
    beSome.like { case p =>
      Visual.addNewLines(Visual.render(situation, Map(p -> 'x'))) must_== visual
    }

  def beSituation(visual: String): Matcher[Validated[String, Situation]] =
    beValid.like { case s =>
      s.visual must_== ((Visual parse visual).get).visual
    }

  def beGame(visual: String): Matcher[Validated[String, Game]] =
    beValid.like { case g =>
      g.situation.visual must_== ((Visual parse visual).get).visual
    }

  def sortPoss(poss: Seq[Pos]): Seq[Pos] = poss sortBy (_.toString)

  def pieceMoves(piece: Piece, pos: Pos): Option[List[Pos]] = {
    val sit = makeEmptySituation
    sit.withBoard(sit.board.place(piece, pos).get).moveActorAt(pos) map (_.destinations)
  }
}
