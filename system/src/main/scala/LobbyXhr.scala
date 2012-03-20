package lila.system

import model._
import memo._
import db.GameRepo
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class LobbyXhr(
    gameRepo: GameRepo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo) {
}
