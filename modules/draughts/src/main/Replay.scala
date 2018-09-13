package draughts

import draughts.format.pdn.San
import draughts.format.{ Forsyth, FEN, Uci }
import format.pdn.{ Parser, Reader, Tag, Tags }
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ failureNel, success }

case class Replay(setup: DraughtsGame, moves: List[Move], state: DraughtsGame) {

  lazy val chronoMoves = moves.reverse

  def addMove(move: Move) = copy(
    moves = move.applyVariantEffect :: moves,
    state = state.apply(move)
  )

  def moveAtPly(ply: Int): Option[Move] =
    chronoMoves lift (ply - 1 - setup.startedAtTurn)
}

object Replay {

  def apply(game: DraughtsGame) = new Replay(game, Nil, game)

  def apply(
    moveStrs: Traversable[String],
    initialFen: Option[String],
    variant: draughts.variant.Variant
  ): Valid[Reader.Result] =
    moveStrs.some.filter(_.nonEmpty) toValid "[replay] pdn is empty" flatMap { nonEmptyMoves =>
      Reader.moves(
        nonEmptyMoves,
        Tags(List(
          initialFen map { fen => Tag(_.FEN, fen) },
          variant.some.filterNot(_.standard) map { v => Tag(_.GameType, v.gameType) }
        ).flatten)
      )
    }

  private def recursiveGames(game: DraughtsGame, sans: List[San]): Valid[List[DraughtsGame]] =
    sans match {
      case Nil => success(Nil)
      case san :: rest => san(game.situation) flatMap { move =>
        val newGame = game.apply(move)
        recursiveGames(newGame, rest) map { newGame :: _ }
      }
    }

  def games(
    moveStrs: Traversable[String],
    initialFen: Option[String],
    variant: draughts.variant.Variant
  ): Valid[List[DraughtsGame]] =
    Parser.moves(moveStrs, variant) flatMap { moves =>
      val game = makeGame(variant, initialFen)
      recursiveGames(game, moves.value) map { game :: _ }
    }

  type ErrorMessage = String
  def gameMoveWhileValid(
    moveStrs: Seq[String],
    initialFen: String,
    variant: draughts.variant.Variant,
    iteratedCapts: Boolean = false
  ): (DraughtsGame, List[(DraughtsGame, Uci.WithSan)], Option[ErrorMessage]) = {

    def mk(g: DraughtsGame, moves: List[(San, String)], ambs: List[(San, String)]): (List[(DraughtsGame, Uci.WithSan)], Option[ErrorMessage]) = {
      var newAmb = none[(San, String)]
      val res = moves match {
        case (san, sanStr) :: rest =>
          san(g.situation, iteratedCapts, if (ambs.isEmpty) None else ambs.collect({ case (ambSan, ambUci) if ambSan == san => ambUci }).some).fold(
            err => (Nil, err.head.some),
            move => {
              val newGame = g(move)
              val uci = move.toUci
              if (iteratedCapts && move.capture.fold(false)(_.lengthCompare(1) > 0) && move.situationBefore.ambiguitiesMove(move) > 0)
                newAmb = (san -> uci.uci).some
              mk(newGame, rest, if (newAmb.isDefined) newAmb.get :: ambs else ambs) match {
                case (next, msg) => ((newGame, Uci.WithSan(uci, sanStr)) :: next, msg)
              }
            }
          )
        case _ => (Nil, None)
      }
      if (res._2.isDefined && newAmb.isDefined) mk(g, moves, newAmb.get :: ambs)
      else res
    }

    val init = makeGame(variant, initialFen.some)
    Parser.moves(moveStrs, variant).fold(
      err => List.empty[(DraughtsGame, Uci.WithSan)] -> err.head.some,
      moves => mk(init, moves.value zip moveStrs, Nil)
    ) match {
        case (games, err) => (init, games, err)
      }
  }

  def exportScanMoves(
    moveStrs: Seq[String],
    initialFen: String,
    variant: draughts.variant.Variant,
    iteratedCapts: Boolean = false
  ): List[String] = {

    def mk(g: DraughtsGame, moves: List[(San, String)], ambs: List[(San, String)]): (List[String], Option[ErrorMessage]) = {
      var newAmb = none[(San, String)]
      val res = moves match {
        case (san, sanStr) :: rest =>
          san(g.situation, iteratedCapts, if (ambs.isEmpty) None else ambs.collect({ case (ambSan, ambUci) if ambSan == san => ambUci }).some).fold(
            err => (Nil, err.head.some),
            move => {
              val newGame = g(move)
              val scanMove = move.toScanMove
              if (iteratedCapts && move.capture.fold(false)(_.lengthCompare(1) > 0) && move.situationBefore.ambiguitiesMove(move) > 0)
                newAmb = (san -> move.toUci.uci).some
              mk(newGame, rest, if (newAmb.isDefined) newAmb.get :: ambs else ambs) match {
                case (next, msg) => (scanMove :: next, msg)
              }
            }
          )
        case _ => (Nil, None)
      }
      if (res._2.isDefined && newAmb.isDefined) mk(g, moves, newAmb.get :: ambs)
      else res
    }

    val init = makeGame(variant, initialFen.some)
    Parser.moves(moveStrs, variant).fold(
      err => List.empty[String] -> err.head.some,
      moves => mk(init, moves.value zip moveStrs, Nil)
    ) match {
        case (moves, None) => moves
        case _ => Nil
      }
  }

  private def recursiveSituations(sit: Situation, sans: List[San]): Valid[List[Situation]] =
    sans match {
      case Nil => success(Nil)
      case san :: rest => san(sit) flatMap { moveOrDrop =>
        val after = Situation(moveOrDrop.afterWithLastMove, !sit.color)
        recursiveSituations(after, rest) map { after :: _ }
      }
    }

  private def recursiveSituationsFromUci(sit: Situation, ucis: List[Uci]): Valid[List[Situation]] =
    ucis match {
      case Nil => success(Nil)
      case uci :: rest => uci(sit) flatMap { moveOrDrop =>
        val after = Situation(moveOrDrop.afterWithLastMove, !sit.color)
        recursiveSituationsFromUci(after, rest) map { after :: _ }
      }
    }

  private def recursiveReplayFromUci(replay: Replay, ucis: List[Uci]): Valid[Replay] =
    ucis match {
      case Nil => success(replay)
      case uci :: rest => uci(replay.state.situation) flatMap { moveOrDrop =>
        recursiveReplayFromUci(replay addMove moveOrDrop, rest)
      }
    }

  private def initialFenToSituation(initialFen: Option[FEN], variant: draughts.variant.Variant): Situation = {
    initialFen.flatMap { fen => Forsyth << fen.value } | Situation(draughts.variant.Standard)
  } withVariant variant

  def boards(
    moveStrs: Traversable[String],
    initialFen: Option[FEN],
    variant: draughts.variant.Variant
  ): Valid[List[Board]] = situations(moveStrs, initialFen, variant) map (_ map (_.board))

  def situations(
    moveStrs: Traversable[String],
    initialFen: Option[FEN],
    variant: draughts.variant.Variant
  ): Valid[List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    Parser.moves(moveStrs, sit.board.variant) flatMap { moves =>
      recursiveSituations(sit, moves.value) map { sit :: _ }
    }
  }

  def boardsFromUci(
    moves: List[Uci],
    initialFen: Option[FEN],
    variant: draughts.variant.Variant
  ): Valid[List[Board]] = situationsFromUci(moves, initialFen, variant) map (_ map (_.board))

  def situationsFromUci(
    moves: List[Uci],
    initialFen: Option[FEN],
    variant: draughts.variant.Variant
  ): Valid[List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    recursiveSituationsFromUci(sit, moves) map { sit :: _ }
  }

  def apply(
    moves: List[Uci],
    initialFen: Option[String],
    variant: draughts.variant.Variant
  ): Valid[Replay] =
    recursiveReplayFromUci(Replay(makeGame(variant, initialFen)), moves)

  def plyAtFen(
    moveStrs: Traversable[String],
    initialFen: Option[String],
    variant: draughts.variant.Variant,
    atFen: String
  ): Valid[Int] =
    if (Forsyth.<<@(variant, atFen).isEmpty) failureNel(s"Invalid FEN $atFen")
    else {

      // we don't want to compare the full move number, to match transpositions
      def truncateFen(fen: String) = fen.split(' ').take(4) mkString " "
      val atFenTruncated = truncateFen(atFen)
      def compareFen(fen: String) = truncateFen(fen) == atFenTruncated

      def recursivePlyAtFen(sit: Situation, sans: List[San], ply: Int): Valid[Int] =
        sans match {
          case Nil => failureNel(s"Can't find $atFenTruncated, reached ply $ply")
          case san :: rest => san(sit) flatMap { move =>
            val after = move.finalizeAfter
            val fen = Forsyth >> DraughtsGame(Situation(after, Color.fromPly(ply)), turns = ply)
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

  private def makeGame(variant: draughts.variant.Variant, initialFen: Option[String]): DraughtsGame = {
    val g = DraughtsGame(variant.some, initialFen)
    g.copy(startedAtTurn = g.turns)
  }
}
