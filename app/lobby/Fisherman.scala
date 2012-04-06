package lila
package lobby

import db.HookRepo
import memo.HookMemo
import model.Hook

import scalaz.effects._

final class Fisherman(
    hookRepo: HookRepo,
    hookMemo: HookMemo,
    socket: Lobby) {

  // DO delete in db
  def -(hook: Hook): IO[Unit] = for {
    _ ← hookRepo removeId hook.id
    _ ← hookMemo remove hook.ownerId
    _ ← socket removeHook hook
  } yield ()

  // DO NOT insert in db (done on php side)
  def +(hook: Hook): IO[Unit] = for {
    _ ← shake(hook)
    _ ← socket addHook hook
  } yield ()

  // mark the hook as active, once
  def shake(hook: Hook): IO[Unit] = hookMemo put hook.ownerId

  def cleanup: IO[Unit] = for {
    hooks ← hookRepo unmatchedNotInOwnerIds hookMemo.keys
    _ ← (hooks map this.-).sequence
  } yield ()
}
