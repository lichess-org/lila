package lila.round

import akka.actor.ActorSelection

import actorApi._
import lila.chat.actorApi._
import lila.chat.Chat
import lila.game.Game
import lila.hub.actorApi.shutup.PublicSource
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.i18n.{ I18nKeys, enLang }
import lila.user.User

final class Messenger(val chat: ActorSelection) {

  def system(game: Game, message: SelectI18nKey, args: Any*): Unit = {
    val translated = message(I18nKeys).literalTxtTo(enLang, args)
    chat ! SystemTalk(Chat.Id(watcherId(game.id)), translated)
    if (game.nonAi) chat ! SystemTalk(Chat.Id(game.id), translated)
  }

  def systemForOwners(gameId: Game.ID, message: SelectI18nKey, args: Any*): Unit = {
    val translated = message(I18nKeys).literalTxtTo(enLang, args)
    chat ! SystemTalk(Chat.Id(gameId), translated)
  }

  def watcher(gameId: Game.ID, userId: User.ID, text: String) =
    chat ! UserTalk(Chat.Id(watcherId(gameId)), userId, text, PublicSource.Watcher(gameId).some)

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(gameId: Game.ID, userId: User.ID, text: String): Unit =
    whisperCommands.collectFirst {
      case command if text startsWith command =>
        val source = PublicSource.Watcher(gameId)
        UserTalk(Chat.Id(watcherId(gameId)), userId, text drop command.size, source.some)
    }.orElse {
      if (text startsWith "/") none // mistyped command?
      else UserTalk(Chat.Id(gameId), userId, text, publicSource = none).some
    } foreach chat.!

  def owner(gameId: Game.ID, anonColor: chess.Color, text: String): Unit =
    chat ! PlayerTalk(Chat.Id(gameId), anonColor.white, text)

  private def watcherId(gameId: Game.ID) = s"$gameId/w"
}
