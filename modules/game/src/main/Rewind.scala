package lila.game

import scala.concurrent.duration._

import chess.{ Color, White, Black }
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
    tags = createTags(initialFen, game)
  ) map { replay =>
      val rewindedGame = replay.state
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      def rewindPlayer(player: Player) = player.copy(proposeTakebackAt = 0)
      def rewindedPlayerMoves(color: Color) = {
        val playedTurns = rewindedGame.turns - game.startedAtTurn
        if (color == game.startColor) (playedTurns + 1) / 2
        else playedTurns / 2
      }
      val newClock = game.clock map (_.takeback)
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
          lastMove = rewindedHistory.lastMove.map(_.origDest),
          lastMoveTime = Some(((nowMillis - game.createdAt.getMillis) / 100).toInt),
          check = if (rewindedSituation.check) rewindedSituation.kingPos else None
        ),
        unmovedRooks = rewindedGame.board.unmovedRooks,
        clockHistory = for {
          history <- game.clockHistory
          clk <- newClock
        } yield ClockHistory(
          (clk.remainingTime(rewindedSituation.color) * 1000).toLong.millis,
          history.white.take(rewindedPlayerMoves(White) - 1),
          history.black.take(rewindedPlayerMoves(Black) - 1)
        ),
        crazyData = rewindedSituation.board.crazyData,
        status = game.status,
        clock = newClock
      )
      Progress(game, newGame, List(
        newGame.clock.map(Event.Clock.apply),
        newGame.playableCorrespondenceClock.map(Event.CorrespondenceClock.apply)
      ).flatten)
    }
}
