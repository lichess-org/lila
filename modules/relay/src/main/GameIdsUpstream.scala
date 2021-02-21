package lila.relay

import chess.format.FEN
import chess.Replay
import scala.concurrent.ExecutionContext

import lila.game.{ Game, GameRepo, PgnDump }

final private class GameIdsUpstream(
    gameRepo: GameRepo,
    pgnDump: PgnDump
)(implicit ec: ExecutionContext) {}
