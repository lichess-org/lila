package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max

final class LobbyXhr(
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    lobbyMemo: LobbyMemo) {
}
