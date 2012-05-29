package lila
package round

import game.{ DbGame, PovRef, Namer }
import i18n.{ I18nKeys, I18nKey, Untranslated }
import I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Color
import Event.Message

import scalaz.effects._

final class Messenger(
    roomRepo: RoomRepo,
    i18nKeys: I18nKeys) {

  def init(game: DbGame): IO[List[Event]] =
    systemMessages(game, initKeys(game))

  private def initKeys(game: DbGame): List[SelectI18nKey] = List(
    Some(game.creatorColor.fold(
      _.whiteCreatesTheGame,
      _.blackCreatesTheGame): SelectI18nKey),
    Some(game.invitedColor.fold(
      _.whiteJoinsTheGame,
      _.blackJoinsTheGame): SelectI18nKey),
    game.clock map { c ⇒ ((_.untranslated(Namer clock c)): SelectI18nKey) },
    game.rated option ((_.thisGameIsRated): SelectI18nKey)
  ).flatten

  def playerMessage(
    ref: PovRef,
    message: String): IO[List[Event]] =
    if (message.size <= 140 && message.nonEmpty)
      roomRepo.addMessage(ref.gameId, ref.color.name, message.replace(""""""", "'")) map { _ ⇒
        List(Message(ref.color.name, message))
      }
    else io(Nil)

  def systemMessages(game: DbGame, messages: List[SelectI18nKey]): IO[List[Event]] =
    game.invited.isHuman.fold(
      (messages map messageToKey) |> { messageKeys ⇒
        roomRepo.addSystemMessages(game.id, messageKeys) map { _ ⇒
          messageKeys map { Message("system", _) }
        }
      },
      io(Nil)
    )

  def systemMessage(game: DbGame, message: SelectI18nKey): IO[List[Event]] =
    game.invited.isHuman.fold(
      messageToKey(message) |> { messageKey ⇒
        roomRepo.addSystemMessage(game.id, messageKey) map { _ ⇒
          List(Message("system", messageKey))
        }
      },
      io(Nil))

  def render(game: DbGame): IO[Option[String]] =
    game.hasAi.fold(io(None), render(game.id) map (_.some))

  def render(roomId: String): IO[String] =
    roomRepo room roomId map (_.render)

  private def messageToKey(message: SelectI18nKey): String =
    message(i18nKeys).key
}
