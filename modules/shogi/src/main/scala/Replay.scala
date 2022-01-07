package shogi

import cats.data.Validated
import cats.data.Validated.{ invalid, valid }
import cats.data.NonEmptyList
import cats.implicits._

import shogi.format.{ FEN, Forsyth, Reader, Tag, Tags }
import shogi.format.usi.Usi

case class Replay(setup: Game, moves: List[MoveOrDrop], state: Game) {

  lazy val chronoMoves = moves.reverse

  def addMove(moveOrDrop: MoveOrDrop) =
    copy(
      moves = moveOrDrop.left.map(_.applyVariantEffect) :: moves,
      state = moveOrDrop.fold(state.apply, state.applyDrop)
    )

  def moveAtPly(ply: Int): Option[MoveOrDrop] =
    chronoMoves lift (ply - 1 - setup.startedAtTurn)
}

object Replay {

  def apply(game: Game) = new Replay(game, Nil, game)

  def apply(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Reader.Result =
    Reader.fromUsi(
      usis,
      Tags(
        List(
          initialFen map { fen =>
            Tag(_.FEN, fen.value)
          },
          variant.some.filterNot(_.standard) map { v =>
            Tag(_.Variant, v.name)
          }
        ).flatten
      )
    )

  def replay(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, Replay] =
    usis.foldLeft[Validated[String, Replay]](valid(Replay(makeGame(variant, initialFen)))) {
      case (acc, usi) =>
        acc andThen { replay =>
          usi(replay.state.situation) flatMap { moveOrDrop =>
            valid(replay addMove moveOrDrop)
          }
        }
    }

  def gamesWhileValid(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): (NonEmptyList[Game], Option[String]) = {

    def mk(g: Game, usis: List[Usi]): (List[Game], Option[String]) =
      usis match {
        case Nil => (Nil, None)
        case usi :: rest =>
          g(usi).fold(
            err => (Nil, err.some),
            gmd => {
              mk(gmd._1, rest) match {
                case (next, msg) => (gmd._1 :: next, msg)
              }
            }
          )
      }

    val init = makeGame(variant, initialFen)
    mk(init, usis.toList) match {
      case (games, err) => (NonEmptyList(init, games), err)
    }
  }

  def boards(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, NonEmptyList[Board]] = situations(usis, initialFen, variant) map (_ map (_.board))

  def situations(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, NonEmptyList[Situation]] = {
    val init = initialFenToSituation(initialFen, variant)
    usis.foldLeft[Validated[String, NonEmptyList[Situation]]](valid(NonEmptyList.one(init))) {
      case (acc, usi) =>
        acc andThen { sits =>
          usi(sits.head) andThen { moveOrDrop =>
            valid(moveOrDrop.fold(_.situationAfter, _.situationAfter) :: sits)
          }
        }
    } map (_.reverse)
  }

  def plyAtFen(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant,
      atFen: FEN
  ): Validated[String, Int] =
    if (Forsyth.<<@(variant, atFen.value).isEmpty) invalid(s"Invalid FEN $atFen")
    else {

      def recursivePlyAtFen(sit: Situation, usis: List[Usi], ply: Int): Validated[String, Int] =
        usis match {
          case Nil => invalid(s"Can't find $atFen, reached ply $ply")
          case usi :: rest =>
            usi(sit) flatMap { moveOrDrop =>
              val after = moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter)
              val fen   = Forsyth >> Game(Situation(after, Color.fromPly(ply)), turns = ply)
              if (Forsyth.compareTruncated(fen, atFen.value)) Validated.valid(ply)
              else recursivePlyAtFen(Situation(after, !sit.color), rest, ply + 1)
            }
        }

      val sit = initialFenToSituation(initialFen, variant)

      recursivePlyAtFen(sit, usis.toList, 1)
    }

  // Use for trusted usis
  // doesn't verify whether the moves are possible
  def usiWithRoleWhilePossible(
      usis: Iterable[Usi],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): List[Usi.WithRole] = {

    def mk(roles: Map[Pos, Role], usis: List[Usi]): List[Usi.WithRole] = {
      usis match {
        case usi :: rest => {
          usi match {
            case Usi.Move(orig, dest, prom) =>
              roles.get(orig).fold[List[Usi.WithRole]](Nil) { role =>
                val maybePromoted = variant.promote(role).filter(_ => prom).getOrElse(role)
                Usi.WithRole(usi, role) ::
                  mk(roles - orig + (dest -> maybePromoted), rest)
              }
            case Usi.Drop(role, pos) =>
              Usi.WithRole(usi, role) :: mk(roles + (pos -> role), rest)
          }
        }
        case _ => Nil
      }
    }

    val init = initialFenToSituation(initialFen, variant)
    mk(init.board.pieces.map { case (k, v) => k -> v.role }, usis.toList)
  }

  private def initialFenToSituation(initialFen: Option[FEN], variant: shogi.variant.Variant): Situation =
    initialFen.flatMap { fen =>
      Forsyth.<<@(variant, fen.value)
    } | Situation(variant)

  private def makeGame(variant: shogi.variant.Variant, initialFen: Option[FEN]): Game =
    Game(variant.some, initialFen.map(_.value))
}
