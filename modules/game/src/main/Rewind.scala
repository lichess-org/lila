package lila.game

import chess.format.{ pgn ⇒ chessPgn }
import chess.Status

object Rewind {

  def apply(
    game: Game,
    moves: List[String],
    initialFen: Option[String]): Valid[(Progress, List[String])] = chessPgn.Reader.withSans(
    pgn = moves mkString " ",
    op = sans ⇒ sans.isEmpty.fold(sans, sans.init),
    tags = initialFen.??(fen ⇒ List(
      chessPgn.Tag(_.FEN, fen),
      chessPgn.Tag(_.Variant, game.variant.name)
    ))
  ) map { replay ⇒
      val rewindedGame = replay.state
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      def rewindPlayer(player: Player) = player.copy(isProposingTakeback = false)
      Progress(game, game.copy(
        whitePlayer = rewindPlayer(game.whitePlayer),
        blackPlayer = rewindPlayer(game.blackPlayer),
        binaryPieces = BinaryFormat.piece write rewindedGame.allPieces,
        turns = rewindedGame.turns,
        positionHashes = rewindedHistory.positionHashes mkString,
        castleLastMoveTime = CastleLastMoveTime(
          castles = rewindedHistory.castles,
          lastMove = rewindedHistory.lastMove,
          lastMoveTime = Some(nowSeconds - game.createdAt.getSeconds.toInt)),
        status =
          if (rewindedSituation.checkMate) Status.Mate
          else if (rewindedSituation.staleMate) Status.Stalemate
          else if (rewindedSituation.autoDraw) Status.Draw
          else game.status,
        clock = game.clock map (_.switch),
        check = if (rewindedSituation.check) rewindedSituation.kingPos else None
      )) -> rewindedGame.pgnMoves
    }
}
