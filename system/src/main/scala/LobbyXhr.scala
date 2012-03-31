package lila.system

import model._
import memo._
import db._
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max

final class LobbyXhr(
    hookRepo: HookRepo,
    lobbyMemo: LobbyMemo,
    hookMemo: HookMemo) {

  def cancel(ownerId: String): IO[Unit] = for {
    _ ← hookRepo removeOwnerId ownerId
    _ ← hookMemo remove ownerId
    _ ← versionInc
  } yield ()

  private[system] def versionInc: IO[Int] = lobbyMemo++
}
