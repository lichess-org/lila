package lila
package game

import chess.Color
import scalaz.effects._

final class Messenger(roomRepo: RoomRepo) {

  def playerMessage(
    ref: PovRef,
    message: String): IO[List[Event]] =
    if (message.size <= 140 && message.nonEmpty)
      roomRepo.addMessage(ref.gameId, ref.color.name, message) map { _ ⇒
        List(MessageEvent(ref.color.name, message))
      }
    else io(Nil)

  def systemMessages(game: DbGame, encodedMessages: String): IO[List[Event]] =
    if (game.invited.isHuman) {
      val messages = (encodedMessages split '$').toList
      roomRepo.addSystemMessages(game.id, messages) map { _ ⇒
        messages map { MessageEvent("system", _) }
      }
    }
    else io(Nil)

  def systemMessage(game: DbGame, message: String): IO[List[Event]] =
    if (game.invited.isHuman)
      roomRepo.addSystemMessage(game.id, message) map { _ ⇒
        List(MessageEvent("system", message))
      }
    else io(Nil)

  def render(roomId: String): IO[String] =
    roomRepo room roomId map (_.render)
}
