package shogi

import cats.data.Validated
import cats.data.Validated.{ invalid, valid }
import cats.implicits._

import shogi.format.{ FEN, Forsyth, ParsedMove, Reader, Tag, Tags, Uci }
import format.pgn.Parser

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
      variant: shogi.variant.Variant
  ): Validated[String, Reader.Result] =
    moveStrs.some.filter(_.nonEmpty) toValid "[replay] pgn is empty" andThen { nonEmptyMoves =>
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

  private def recursiveGames(game: Game, parsedMoves: List[ParsedMove]): Validated[String, List[Game]] =
    parsedMoves match {
      case Nil => valid(Nil)
      case parsedMove :: rest =>
        parsedMove(game.situation) flatMap { moveOrDrop =>
          val newGame = moveOrDrop.fold(game.apply, game.applyDrop)
          recursiveGames(newGame, rest) map { newGame :: _ }
        }
    }

  def games(
      moveStrs: Iterable[String],
      initialFen: Option[String],
      variant: shogi.variant.Variant
  ): Validated[String, List[Game]] =
    Parser.moves(moveStrs, variant) andThen { moves =>
      val game = makeGame(variant, initialFen)
      recursiveGames(game, moves.value) map { game :: _ }
    }

  def gameMoveWhileValid(
      moveStrs: Seq[String],
      initialFen: String,
      variant: shogi.variant.Variant
  ): (Game, List[(Game, Uci.WithSan)], Option[String]) = {
    def mk(g: Game, moves: List[(ParsedMove, String)]): (List[(Game, Uci.WithSan)], Option[String]) = {
      moves match {
        case (san, sanStr) :: rest =>
          san(g.situation).fold(
            err => (Nil, err.some),
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
        err => List.empty[(Game, Uci.WithSan)] -> err.some,
        moves => mk(init, moves.value zip moveStrs)
      ) match {
      case (games, err) => (init, games, err)
    }
  }

  private def recursiveSituations(
      sit: Situation,
      parsedMoves: List[ParsedMove]
  ): Validated[String, List[Situation]] =
    parsedMoves match {
      case Nil => valid(Nil)
      case parsedMove :: rest =>
        parsedMove(sit) flatMap { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter), !sit.color)
          recursiveSituations(after, rest) map { after :: _ }
        }
    }

  private def recursiveSituationsFromUci(
      sit: Situation,
      ucis: List[Uci]
  ): Validated[String, List[Situation]] =
    ucis match {
      case Nil => valid(Nil)
      case uci :: rest =>
        uci(sit) andThen { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter), !sit.color)
          recursiveSituationsFromUci(after, rest) map { after :: _ }
        }
    }

  private def recursiveReplayFromUci(replay: Replay, ucis: List[Uci]): Validated[String, Replay] =
    ucis match {
      case Nil => valid(replay)
      case uci :: rest =>
        uci(replay.state.situation) andThen { moveOrDrop =>
          recursiveReplayFromUci(replay addMove moveOrDrop, rest)
        }
    }

  private def initialFenToSituation(initialFen: Option[FEN], variant: shogi.variant.Variant): Situation = {
    initialFen.flatMap { fen =>
      Forsyth << fen.value
    } | Situation(shogi.variant.Standard)
  } withVariant variant

  def boards(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, List[Board]] = situations(moveStrs, initialFen, variant) map (_ map (_.board))

  def situations(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    Parser.moves(moveStrs, sit.board.variant) andThen { moves =>
      recursiveSituations(sit, moves.value) map { sit :: _ }
    }
  }

  def boardsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, List[Board]] = situationsFromUci(moves, initialFen, variant) map (_ map (_.board))

  def situationsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: shogi.variant.Variant
  ): Validated[String, List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    recursiveSituationsFromUci(sit, moves) map { sit :: _ }
  }

  def apply(
      moves: List[Uci],
      initialFen: Option[String],
      variant: shogi.variant.Variant
  ): Validated[String, Replay] =
    recursiveReplayFromUci(Replay(makeGame(variant, initialFen)), moves)

  def plyAtFen(
      moveStrs: Iterable[String],
      initialFen: Option[String],
      variant: shogi.variant.Variant,
      atFen: String
  ): Validated[String, Int] =
    if (Forsyth.<<@(variant, atFen).isEmpty) invalid(s"Invalid FEN $atFen")
    else {

      // we don't want to compare the full move number, to match transpositions
      def truncateFen(fen: String) = fen.split(' ').take(3) mkString " "
      val atFenTruncated           = truncateFen(atFen)
      def compareFen(fen: String)  = truncateFen(fen) == atFenTruncated

      def recursivePlyAtFen(sit: Situation, parsedMoves: List[ParsedMove], ply: Int): Validated[String, Int] =
        parsedMoves match {
          case Nil => invalid(s"Can't find $atFenTruncated, reached ply $ply")
          case san :: rest =>
            san(sit) flatMap { moveOrDrop =>
              val after = moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter)
              val fen   = Forsyth >> Game(Situation(after, Color.fromPly(ply)), turns = ply)
              if (compareFen(fen)) Validated.valid(ply)
              else recursivePlyAtFen(Situation(after, !sit.color), rest, ply + 1)
            }
        }

      val sit = initialFen.flatMap {
        Forsyth.<<@(variant, _)
      } | Situation(variant)

      Parser.moves(moveStrs, sit.board.variant) andThen { moves =>
        recursivePlyAtFen(sit, moves.value, 1)
      }
    }

  private def makeGame(variant: shogi.variant.Variant, initialFen: Option[String]): Game = {
    val g = Game(variant.some, initialFen)
    g.copy(startedAtTurn = g.turns)
  }
}
