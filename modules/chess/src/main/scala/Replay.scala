package chess

import chess.format.pgn.San
import chess.format.{ FEN, Forsyth, Uci }
import format.pgn.{ Parser, Reader, Tag, Tags }
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ failureNel, success }

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
      moveStrs: Iterable[String],
      initialFen: Option[String],
      variant: chess.variant.Variant
  ): Valid[Reader.Result] =
    moveStrs.some.filter(_.nonEmpty) toValid "[replay] pgn is empty" flatMap { nonEmptyMoves =>
      Reader.moves(
        nonEmptyMoves,
        Tags(
          List(
            initialFen map { fen =>
              Tag(_.FEN, fen)
            },
            variant.some.filterNot(_.standard) map { v =>
              Tag(_.Variant, v.name)
            }
          ).flatten
        )
      )
    }

  private def recursiveGames(game: Game, sans: List[San]): Valid[List[Game]] =
    sans match {
      case Nil => success(Nil)
      case san :: rest =>
        san(game.situation) flatMap { moveOrDrop =>
          val newGame = moveOrDrop.fold(game.apply, game.applyDrop)
          recursiveGames(newGame, rest) map { newGame :: _ }
        }
    }

  def games(
      moveStrs: Iterable[String],
      initialFen: Option[String],
      variant: chess.variant.Variant
  ): Valid[List[Game]] =
    Parser.moves(moveStrs, variant) flatMap { moves =>
      val game = makeGame(variant, initialFen)
      recursiveGames(game, moves.value) map { game :: _ }
    }

  type ErrorMessage = String
  def gameMoveWhileValid(
      moveStrs: Seq[String],
      initialFen: String,
      variant: chess.variant.Variant
  ): (Game, List[(Game, Uci.WithSan)], Option[ErrorMessage]) = {
    def mk(g: Game, moves: List[(San, String)]): (List[(Game, Uci.WithSan)], Option[ErrorMessage]) = {
      moves match {
        case (san, sanStr) :: rest =>
          san(g.situation).fold(
            err => (Nil, err.head.some),
            moveOrDrop => {
              val newGame = moveOrDrop.fold(g.apply, g.applyDrop)
              val uci     = moveOrDrop.fold(_.toUci, _.toUci)
              mk(newGame, rest) match {
                case (next, msg) => ((newGame, Uci.WithSan(uci, sanStr)) :: next, msg)
              }
            }
          )
        case _ => (Nil, None)
      }
    }
    val init = makeGame(variant, initialFen.some)
    Parser
      .moves(moveStrs, variant)
      .fold(
        err => List.empty[(Game, Uci.WithSan)] -> err.head.some,
        moves => mk(init, moves.value zip moveStrs)
      ) match {
      case (games, err) => (init, games, err)
    }
  }

  private def recursiveSituations(sit: Situation, sans: List[San]): Valid[List[Situation]] =
    sans match {
      case Nil => success(Nil)
      case san :: rest =>
        san(sit) flatMap { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(_.afterWithLastMove, _.afterWithLastMove), !sit.color)
          recursiveSituations(after, rest) map { after :: _ }
        }
    }

  private def recursiveSituationsFromUci(sit: Situation, ucis: List[Uci]): Valid[List[Situation]] =
    ucis match {
      case Nil => success(Nil)
      case uci :: rest =>
        uci(sit) flatMap { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(_.afterWithLastMove, _.afterWithLastMove), !sit.color)
          recursiveSituationsFromUci(after, rest) map { after :: _ }
        }
    }

  private def recursiveReplayFromUci(replay: Replay, ucis: List[Uci]): Valid[Replay] =
    ucis match {
      case Nil => success(replay)
      case uci :: rest =>
        uci(replay.state.situation) flatMap { moveOrDrop =>
          recursiveReplayFromUci(replay addMove moveOrDrop, rest)
        }
    }

  private def initialFenToSituation(initialFen: Option[FEN], variant: chess.variant.Variant): Situation = {
    initialFen.flatMap { fen =>
      Forsyth << fen.value
    } | Situation(chess.variant.Standard)
  } withVariant variant

  def boards(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: chess.variant.Variant
  ): Valid[List[Board]] = situations(moveStrs, initialFen, variant) map (_ map (_.board))

  def situations(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: chess.variant.Variant
  ): Valid[List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    Parser.moves(moveStrs, sit.board.variant) flatMap { moves =>
      recursiveSituations(sit, moves.value) map { sit :: _ }
    }
  }

  def boardsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: chess.variant.Variant
  ): Valid[List[Board]] = situationsFromUci(moves, initialFen, variant) map (_ map (_.board))

  def situationsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: chess.variant.Variant
  ): Valid[List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    recursiveSituationsFromUci(sit, moves) map { sit :: _ }
  }

  def apply(
      moves: List[Uci],
      initialFen: Option[String],
      variant: chess.variant.Variant
  ): Valid[Replay] =
    recursiveReplayFromUci(Replay(makeGame(variant, initialFen)), moves)

  def plyAtFen(
      moveStrs: Iterable[String],
      initialFen: Option[String],
      variant: chess.variant.Variant,
      atFen: String
  ): Valid[Int] =
    if (Forsyth.<<@(variant, atFen).isEmpty) failureNel(s"Invalid FEN $atFen")
    else {

      // we don't want to compare the full move number, to match transpositions
      def truncateFen(fen: String) = fen.split(' ').take(4) mkString " "
      val atFenTruncated           = truncateFen(atFen)
      def compareFen(fen: String)  = truncateFen(fen) == atFenTruncated

      def recursivePlyAtFen(sit: Situation, sans: List[San], ply: Int): Valid[Int] =
        sans match {
          case Nil => failureNel(s"Can't find $atFenTruncated, reached ply $ply")
          case san :: rest =>
            san(sit) flatMap { moveOrDrop =>
              val after = moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter)
              val fen   = Forsyth >> Game(Situation(after, Color.fromPly(ply)), turns = ply)
              if (compareFen(fen)) scalaz.Success(ply)
              else recursivePlyAtFen(Situation(after, !sit.color), rest, ply + 1)
            }
        }

      val sit = initialFen.flatMap {
        Forsyth.<<@(variant, _)
      } | Situation(variant)

      Parser.moves(moveStrs, sit.board.variant) flatMap { moves =>
        recursivePlyAtFen(sit, moves.value, 1)
      }
    }

  private def makeGame(variant: chess.variant.Variant, initialFen: Option[String]): Game = {
    val g = Game(variant.some, initialFen)
    g.copy(startedAtTurn = g.turns)
  }
}
