package lila.game

import chess.format.{ pgn => chessPgn }

object Rewind {

  private def createTags(fen: Option[String], game: Game) = {
    val variantTag = Some(chessPgn.Tag(_.Variant, game.variant.name))
    val fenTag = fen map (fenString => chessPgn.Tag(_.FEN, fenString))

    List(variantTag, fenTag).flatten
  }

  def apply(game: Game, initialFen: Option[String]): Valid[Progress] = chessPgn.Reader.movesWithSans(
    moveStrs = game.pgnMoves,
    op = sans => sans.isEmpty.fold(sans, sans.init),
    tags = createTags(initialFen, game)) map { replay =>
      val rewindedGame = replay.state
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      def rewindPlayer(player: Player) = player.copy(isProposingTakeback = false)
      val newGame = game.copy(
        whitePlayer = rewindPlayer(game.whitePlayer),
        blackPlayer = rewindPlayer(game.blackPlayer),
        binaryPieces = BinaryFormat.piece write rewindedGame.board.pieces,
        binaryPgn = BinaryFormat.pgn write rewindedGame.pgnMoves,
        turns = rewindedGame.turns,
        positionHashes = rewindedHistory.positionHashes,
        checkCount = rewindedHistory.checkCount,
        castleLastMoveTime = CastleLastMoveTime(
          castles = rewindedHistory.castles,
          lastMove = rewindedHistory.lastMove,
          lastMoveTime = Some(((nowMillis - game.createdAt.getMillis) / 100).toInt),
          check = if (rewindedSituation.check) rewindedSituation.kingPos else None),
        binaryMoveTimes = BinaryFormat.moveTime write (game.moveTimes take rewindedGame.turns),
        status = game.status,
        clock = game.clock map (_.takeback))
      Progress(game, newGame, List(
        newGame.clock.map(Event.Clock.apply),
        newGame.correspondenceClock.map(Event.CorrespondenceClock.apply)
      ).flatten)
    }
}
