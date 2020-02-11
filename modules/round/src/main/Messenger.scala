package lila.round

import lila.chat.{ Chat, ChatApi, ChatTimeout }
import lila.game.Game
import lila.hub.actorApi.shutup.PublicSource
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.i18n.{ enLang, I18nKeys }
import lila.user.User

final class Messenger(api: ChatApi) {

  def system(game: Game, message: String): Unit =
    system(true)(game, message)

  def volatile(game: Game, message: String): Unit =
    system(false)(game, message)

  def system(persistent: Boolean)(game: Game, message: String): Unit = {
    val apiCall =
      if (persistent) api.userChat.system _
      else api.userChat.volatile _
    apiCall(watcherId(Chat.Id(game.id)), message)
    if (game.nonAi) apiCall(Chat.Id(game.id), message)
  }

  def systemForOwners(chatId: Chat.Id, message: String): Unit = {
    api.userChat.system(chatId, message)
  }

  def watcher(chatId: Chat.Id, userId: User.ID, text: String) =
    api.userChat.write(watcherId(chatId), userId, text, PublicSource.Watcher(chatId.value).some)

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(chatId: Chat.Id, userId: User.ID, text: String): Unit =
    whisperCommands.collectFirst {
      case command if text startsWith command =>
        val source = PublicSource.Watcher(chatId.value)
        api.userChat.write(watcherId(chatId), userId, text drop command.size, source.some)
    } getOrElse {
      if (!text.startsWith("/")) // mistyped command?
        api.userChat.write(chatId, userId, text, publicSource = none).some
    }

  def owner(chatId: Chat.Id, anonColor: chess.Color, text: String): Unit =
    api.playerChat.write(chatId, anonColor, text)

  // simul or tour chat from a game
  def external(setup: Chat.Setup, userId: User.ID, text: String): Unit =
    api.userChat.write(setup.id, userId, text, setup.publicSource.some)

  def timeout(chatId: Chat.Id, modId: User.ID, suspect: User.ID, reason: String): Unit =
    ChatTimeout.Reason(reason) foreach { r =>
      api.userChat.timeout(chatId, modId, suspect, r, local = false)
    }

  private def watcherId(chatId: Chat.Id) = Chat.Id(s"$chatId/w")
}
