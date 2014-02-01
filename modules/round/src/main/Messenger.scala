package lila.round

import akka.actor.ActorRef

import lila.chat.actorApi._
import lila.common.Bus
import lila.game.Game
import actorApi._
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.i18n.I18nKeys

final class Messenger(
    bus: Bus,
    socketHub: akka.actor.ActorRef,
    i18nKeys: I18nKeys) {

  def system(game: Game, message: SelectI18nKey, args: Any*) {
    val translated = message(i18nKeys).en(args: _*)
    bus.publish(SystemTalk(game.id + "/w", translated, socketHub), 'chatIn)
    if (game.nonAi) bus.publish(SystemTalk(game.id, translated, socketHub), 'chatIn)
  }

  def watcher(gameId: String, member: Member, text: String, socket: ActorRef) =
    member.userId foreach { userId ⇒
      bus.publish(UserTalk(gameId + "/w", userId, text, socket), 'chatIn)
    }

  def owner(gameId: String, member: Member, text: String, socket: ActorRef) =
    bus.publish(member.userId match {
      case Some(userId) ⇒ UserTalk(gameId, userId, text, socket)
      case None         ⇒ PlayerTalk(gameId, member.color.white, text, socket)
    }, 'chatIn)
}
