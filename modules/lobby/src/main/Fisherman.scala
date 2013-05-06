package lila.lobby

import actorApi._
import lila.game.Game
import lila.memo.ExpireSetMemo
import tube.hookTube
import lila.db.api._

import akka.actor.ActorRef

final class Fisherman(
    hookMemo: ExpireSetMemo,
    socket: ActorRef) {

  def delete(hook: Hook): Funit =
    $remove(hook) >>- (socket ! RemoveHook(hook))

  def add(hook: Hook): Funit =
    $insert(hook) >>- (socket ! AddHook(hook)) >>- shake(hook)

  def bite(hook: Hook, game: Game): Funit =
    HookRepo.setGame(hook, game) >>-
      (socket ! RemoveHook(hook)) >>-
      (socket ! BiteHook(hook, game))

  // mark the hook as active, once
  def shake(hook: Hook) { hookMemo put hook.ownerId }

  def cleanup: Funit =
    HookRepo unmatchedNotInOwnerIds hookMemo.keys flatMap { hooks ⇒
      (hooks map delete).sequence.void
    }
}
