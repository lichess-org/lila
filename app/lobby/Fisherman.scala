package lila
package lobby

import game.DbGame

import scalaz.effects._

final class Fisherman(
    hookRepo: HookRepo,
    hookMemo: HookMemo,
    socket: Socket) {

  def delete(hook: Hook): IO[Unit] = for {
    _ ← socket removeHook hook
    _ ← hookRepo removeId hook.id
  } yield ()

  def add(hook: Hook): IO[Unit] = for {
    _ ← io(hookRepo insert hook)
    _ ← socket addHook hook
    _ ← shake(hook)
  } yield ()

  def bite(hook: Hook, game: DbGame): IO[Unit] = for {
    _ ← socket removeHook hook
    _ ← socket.biteHook(hook, game)
    _ ← hookRepo.setGame(hook, game)
  } yield ()

  // mark the hook as active, once
  def shake(hook: Hook): IO[Unit] = hookMemo put hook.ownerId

  def cleanup: IO[Unit] = for {
    hooks ← hookRepo unmatchedNotInOwnerIds hookMemo.keys
    _ ← (hooks map delete).sequence
  } yield ()
}
