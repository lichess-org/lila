package lila.system

import lila.chess.Color
import model._
import db.{ RoomRepo }
import scalaz.effects._

final class Messenger(roomRepo: RoomRepo) {

  def playerMessage(game: DbGame, color: Color, message: String): IO[DbGame] =
    if (game.invited.isHuman && message.size <= 140 && message.nonEmpty)
      roomRepo.addMessage(game.id, color.name, message) map { _ ⇒
        game withEvents List(MessageEvent(color.name, message))
      }
    else io(game)

  def systemMessages(game: DbGame, encodedMessages: String): IO[DbGame] =
    if (game.invited.isHuman) {
      val messages = (encodedMessages split '$').toList
      roomRepo.addSystemMessages(game.id, messages) map { _ ⇒
        game withEvents (messages map { msg ⇒ MessageEvent("system", msg) })
      }
    }
    else io(game)

  def systemMessage(game: DbGame, message: String): IO[DbGame] =
    if (game.invited.isHuman)
      roomRepo.addSystemMessage(game.id, message) map { _ ⇒
        game withEvents List(MessageEvent("system", message))
      }
    else io(game)

  def render(roomId: String): IO[String] =
    roomRepo room roomId map (_.render)
}
