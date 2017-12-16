package lila.round

import akka.actor.ActorSelection

import actorApi._
import lila.chat.Chat
import lila.chat.actorApi._
import lila.game.Game
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.i18n.{ I18nKeys, enLang }
import lila.hub.actorApi.shutup.PublicSource

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

  def watcher(gameId: Game.ID, member: Member, text: String) =
    member.userId foreach { userId =>
      val source = PublicSource.Watcher(gameId)
      chat ! UserTalk(Chat.Id(watcherId(gameId)), userId, text, source.some)
    }

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(gameId: Game.ID, member: Member, text: String) = (member.userId match {
    case Some(userId) =>
      whisperCommands.collectFirst {
        case command if text startsWith command =>
          val source = PublicSource.Watcher(gameId)
          UserTalk(Chat.Id(watcherId(gameId)), userId, text drop command.size, source.some)
      } orElse {
        if (text startsWith "/") none // mistyped command?
        else UserTalk(Chat.Id(gameId), userId, text, publicSource = none).some
      }
    case None =>
      PlayerTalk(Chat.Id(gameId), member.color.white, text).some
  }) foreach chat.!

  private def watcherId(gameId: Game.ID) = s"$gameId/w"
}
