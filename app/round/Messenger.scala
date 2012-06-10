package lila
package round

import game.{ DbGame, PovRef, Namer }
import i18n.{ I18nKeys, I18nKey, Untranslated }
import I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Color
import Event.{ Message, WatcherMessage }

import scalaz.effects._

final class Messenger(
    roomRepo: RoomRepo,
    watcherRoomRepo: WatcherRoomRepo,
    i18nKeys: I18nKeys) {

  def init(game: DbGame): IO[List[Event]] =
    systemMessages(game, initKeys(game))

  private def initKeys(game: DbGame): List[SelectI18nKey] = List(
    game.creatorColor.fold(_.whiteCreatesTheGame, _.blackCreatesTheGame),
    game.invitedColor.fold(_.whiteJoinsTheGame, _.blackJoinsTheGame)
  )

  def playerMessage(ref: PovRef, text: String): IO[List[Event]] =
    cleanupText(text).fold(
      t ⇒ roomRepo.addMessage(ref.gameId, ref.color.name, t) map { _ ⇒
        List(Message(ref.color.name, t))
      },
      io(Nil)
    )

  def watcherMessage(
    gameId: String,
    username: Option[String],
    text: String): IO[List[Event]] =
    cleanupText(text).fold(
      t ⇒ watcherRoomRepo.addMessage(gameId, username, t) map { msg ⇒
        List(WatcherMessage(msg))
      },
      io(Nil)
    )

  def systemMessages(game: DbGame, messages: List[SelectI18nKey]): IO[List[Event]] =
    game.invited.isHuman.fold(
      (messages map messageToEn) |> { messageKeys ⇒
        roomRepo.addSystemMessages(game.id, messageKeys) map { _ ⇒
          messageKeys map { Message("system", _) }
        }
      },
      io(Nil)
    )

  def systemMessage(game: DbGame, message: SelectI18nKey): IO[List[Event]] =
    game.invited.isHuman.fold(
      messageToEn(message) |> { messageKey ⇒
        roomRepo.addSystemMessage(game.id, messageKey) map { _ ⇒
          List(Message("system", messageKey))
        }
      },
      io(Nil))

  def render(game: DbGame): IO[Option[String]] =
    game.hasAi.fold(io(None), render(game.id) map (_.some))

  def render(roomId: String): IO[String] =
    roomRepo room roomId map (_.render)

  def renderWatcher(game: DbGame): IO[String] =
    watcherRoomRepo room game.id map (_.render)

  private def cleanupText(text: String) = {
    val cleanedUp = text.trim.replace(""""""", "'")
    (cleanedUp.size <= 140 && cleanedUp.nonEmpty) option cleanedUp
  }

  private def messageToEn(message: SelectI18nKey): String =
    message(i18nKeys).en()
}
