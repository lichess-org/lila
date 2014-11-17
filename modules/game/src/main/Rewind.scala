package lila.game

import chess.format.{ pgn => chessPgn }

object Rewind {

  def apply(game: Game, initialFen: Option[String]): Valid[Progress] = chessPgn.Reader.movesWithSans(
    moveStrs = game.pgnMoves,
    op = sans => sans.isEmpty.fold(sans, sans.init),
    tags = initialFen.??(fen => List(
      chessPgn.Tag(_.FEN, fen),
      chessPgn.Tag(_.Variant, game.variant.name)
    )),
    trusted = true
  ) map { replay =>
      val rewindedGame = replay.state
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      def rewindPlayer(player: Player) = player.copy(isProposingTakeback = false)
      Progress(game, game.copy(
        whitePlayer = rewindPlayer(game.whitePlayer),
        blackPlayer = rewindPlayer(game.blackPlayer),
        binaryPieces = BinaryFormat.piece write rewindedGame.board.pieces,
        binaryPgn = BinaryFormat.pgn write rewindedGame.pgnMoves,
        turns = rewindedGame.turns,
        positionHashes = rewindedHistory.positionHashes,
        castleLastMoveTime = CastleLastMoveTime(
          castles = rewindedHistory.castles,
          lastMove = rewindedHistory.lastMove,
          lastMoveTime = Some(nowSeconds - game.createdAt.getSeconds.toInt),
          check = if (rewindedSituation.check) rewindedSituation.kingPos else None),
        binaryMoveTimes = BinaryFormat.moveTime write (game.moveTimes take rewindedGame.turns),
        status = game.status,
        clock = game.clock map (_.switch)
      ))
    }
}
