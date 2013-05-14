package lila.round

import lila.game.{ Game, PovRef, Namer }
import lila.i18n.{ I18nKeys, I18nKey, Untranslated }
import I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Color
import lila.game.Event
import tube.{ roomTube, watcherRoomTube }
import lila.db.api._
import lila.user.UserRepo

import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Messenger(
    val netDomain: String,
    i18nKeys: I18nKeys,
    getUsername: String ⇒ Fu[Option[String]]) extends lila.user.Room {

  private val nbMessagesCopiedToRematch = 20

  def init(game: Game): Fu[List[Event]] =
    systemMessages(game, List(
      game.creatorColor.fold(_.whiteCreatesTheGame, _.blackCreatesTheGame),
      game.invitedColor.fold(_.whiteJoinsTheGame, _.blackJoinsTheGame)
    ))

  // // copies chats then init
  // // no need to send events back
  def rematch(prev: Game, next: Game): Funit = for {
    prevR ← RoomRepo room prev.id
    nextR = prevR.rematchCopy(next.id, nbMessagesCopiedToRematch)
    _ ← nextR.nonEmpty ?? $insert(nextR)
    _ ← systemMessage(next, _.rematchOfferAccepted)
    prevWR ← WatcherRoomRepo room prev.id
    nextWR = prevWR.rematchCopy(next.id, nbMessagesCopiedToRematch)
    _ ← nextWR.nonEmpty ?? $insert(nextWR)
  } yield ()

  def playerMessage(ref: PovRef, text: String): Fu[List[Event.Message]] =
    cleanupText(text).future flatMap { t ⇒
      RoomRepo.addMessage(ref.gameId, ref.color.name, t) inject {
        Event.Message(ref.color.name, t) :: Nil
      }
    }

  def watcherMessage(
    gameId: String,
    userId: Option[String],
    text: String): Fu[List[Event.WatcherMessage]] = for {
    userOption ← userId.zmap(UserRepo.byId)
    message ← userOrAnonMessage(userOption, text).future
    (u, t) = message
    _ ← WatcherRoomRepo.addMessage(gameId, u, t)
  } yield Event.WatcherMessage(u, t) :: Nil

  def systemMessages(game: Game, messages: List[SelectI18nKey]): Fu[List[Event]] =
    game.hasChat ?? {
      (messages map messageToEn) |> { messageKeys ⇒
        RoomRepo.addSystemMessages(game.id, messageKeys) inject {
          messageKeys map { Event.Message("system", _) }
        }
      }
    }

  def systemMessage(game: Game, message: SelectI18nKey): Fu[List[Event]] =
    game.hasChat ?? {
      messageToEn(message) |> { messageKey ⇒
        RoomRepo.addSystemMessage(game.id, messageKey) inject {
          Event.Message("system", messageKey) :: Nil
        }
      }
    }

  private def messageToEn(message: SelectI18nKey): String =
    message(i18nKeys).en()
}
