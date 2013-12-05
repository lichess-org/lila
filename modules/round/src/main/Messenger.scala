package lila.round

import chess.Color
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

import lila.db.api._
import lila.game.Event
import lila.game.{ Game, PovRef, Namer }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.i18n.{ I18nKeys, I18nKey, Untranslated }
import lila.user.UserRepo
import tube.{ roomTube, watcherRoomTube }

final class Messenger(
    val netDomain: String,
    i18nKeys: I18nKeys,
    getUsername: String ⇒ Fu[Option[String]]) extends lila.user.Room {

  private val nbMessagesCopiedToRematch = 20

  def init(game: Game): Fu[List[Event]] = fuccess(Nil)

  // copies chats then init
  // no need to send events back
  def rematch(prev: Game, next: Game): Funit = for {
    prevR ← RoomRepo room prev.id
    nextR = prevR.rematchCopy(next.id, nbMessagesCopiedToRematch)
    _ ← nextR.nonEmpty ?? $insert(nextR)
    _ ← systemMessage(next, _.rematchOfferAccepted)
    prevWR ← WatcherRoomRepo room prev.id
    nextWR = prevWR.rematchCopy(next.id, nbMessagesCopiedToRematch)
    _ ← nextWR.nonEmpty ?? $insert(nextWR)
  } yield ()

  def playerMessage(ref: PovRef, text: String, troll: Boolean): Fu[List[Event.Message]] =
    cleanupText(text).future flatMap { t ⇒
      (!troll ?? RoomRepo.addMessage(ref.gameId, ref.color.name, t)) inject {
        Event.Message(ref.color.name, t, troll) :: Nil
      }
    }

  def watcherMessage(
    roomId: String,
    userId: Option[String],
    text: String,
    troll: Boolean): Fu[List[Event.WatcherMessage]] = for {
    userOption ← userId.??(UserRepo.byId)
    message ← userOrAnonMessage(userOption, text).future
    (u, t) = message
    _ ← !troll ?? WatcherRoomRepo.addMessage(roomId, u, t)
  } yield Event.WatcherMessage(u, t, troll) :: Nil

  def systemMessages(game: Game, messages: List[SelectI18nKey]): Fu[List[Event]] =
    game.hasChat ?? {
      (messages map { m ⇒ trans(m) }) |> { messageKeys ⇒
        RoomRepo.addSystemMessages(game.id, messageKeys) inject {
          messageKeys map { Event.Message("system", _, false) }
        }
      }
    }

  def systemMessage(game: Game, message: SelectI18nKey): Fu[List[Event]] =
    game.hasChat ?? {
      trans(message) |> { messageKey ⇒
        RoomRepo.addSystemMessage(game.id, messageKey) inject {
          Event.Message("system", messageKey, false) :: Nil
        }
      }
    }

  private def trans(message: SelectI18nKey, args: Any*): String =
    message(i18nKeys).en(args: _*)
}
