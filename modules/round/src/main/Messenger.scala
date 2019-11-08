package lila.round

import akka.actor.ActorSelection

import actorApi._
import lila.chat.actorApi._
import lila.chat.{ Chat, ChatTimeout }
import lila.game.Game
import lila.hub.actorApi.shutup.PublicSource
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.i18n.{ I18nKeys, enLang }
import lila.user.User

final class Messenger(val chat: ActorSelection) {

  def system(game: Game, message: SelectI18nKey, args: Any*): Unit = {
    val translated = message(I18nKeys).literalTxtTo(enLang, args)
    chat ! SystemTalk(watcherId(Chat.Id(game.id)), translated)
    if (game.nonAi) chat ! SystemTalk(Chat.Id(game.id), translated)
  }

  def systemForOwners(chatId: Chat.Id, message: SelectI18nKey, args: Any*): Unit = {
    val translated = message(I18nKeys).literalTxtTo(enLang, args)
    chat ! SystemTalk(chatId, translated)
  }

  def watcher(chatId: Chat.Id, userId: User.ID, text: String) =
    chat ! UserTalk(watcherId(chatId), userId, text, PublicSource.Watcher(chatId.value).some)

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(chatId: Chat.Id, userId: User.ID, text: String): Unit =
    whisperCommands.collectFirst {
      case command if text startsWith command =>
        val source = PublicSource.Watcher(chatId.value)
        UserTalk(watcherId(chatId), userId, text drop command.size, source.some)
    }.orElse {
      if (text startsWith "/") none // mistyped command?
      else UserTalk(chatId, userId, text, publicSource = none).some
    } foreach chat.!

  def owner(chatId: Chat.Id, anonColor: chess.Color, text: String): Unit =
    chat ! PlayerTalk(chatId, anonColor.white, text)

  // simul or tour chat from a game
  def external(setup: Chat.Setup, userId: User.ID, text: String): Unit =
    chat ! UserTalk(setup.id, userId, text, setup.publicSource.some)

  def timeout(chatId: Chat.Id, modId: User.ID, suspect: User.ID, reason: String): Unit =
    ChatTimeout.Reason(reason) foreach { r =>
      chat ! Timeout(chatId, modId, suspect, r, local = false)
    }

  private def watcherId(chatId: Chat.Id) = Chat.Id(s"$chatId/w")
}
