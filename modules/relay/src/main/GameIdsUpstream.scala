package lila.relay

import scala.concurrent.ExecutionContext

import lila.game.{ GameRepo, PgnDump }

final private class GameIdsUpstream(
    gameRepo: GameRepo,
    pgnDump: PgnDump
)(implicit ec: ExecutionContext) {}
