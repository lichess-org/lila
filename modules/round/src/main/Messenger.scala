package lila.round

import akka.actor.ActorSelection

import actorApi._
import lila.chat.actorApi._
import lila.game.Game
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.i18n.I18nKeys

final class Messenger(val chat: ActorSelection) {

  def system(game: Game, message: SelectI18nKey, args: Any*) {
    val translated = message(I18nKeys).en(args: _*)
    chat ! SystemTalk(watcherId(game.id), translated)
    if (game.nonAi) chat ! SystemTalk(game.id, translated)
  }

  def systemForOwners(gameId: String, message: SelectI18nKey, args: Any*) {
    val translated = message(I18nKeys).en(args: _*)
    chat ! SystemTalk(gameId, translated)
  }

  def watcher(gameId: String, member: Member, text: String) =
    member.userId foreach { userId =>
      chat ! UserTalk(watcherId(gameId), userId, text)
    }

  private val whisperCommands = List("/whisper ", "/w ")

  def owner(gameId: String, member: Member, text: String) = (member.userId match {
    case Some(userId) =>
      whisperCommands.collectFirst {
        case command if text startsWith command =>
          UserTalk(watcherId(gameId), userId, text drop command.size)
      } orElse {
        if (text startsWith "/") none // mistyped command?
        else UserTalk(gameId, userId, text, public = false).some
      }
    case None =>
      PlayerTalk(gameId, member.color.white, text).some
  }) foreach chat.!

  private def watcherId(gameId: String) = s"$gameId/w"
}
