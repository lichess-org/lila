package lila.round

import lila.game.{ Game, PovRef, Namer }
import lila.i18n.{ I18nKeys, I18nKey, Untranslated }
import I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Color
import lila.game.Event
import tube.{ roomTube, watcherRoomTube }
import lila.db.api._

final class Messenger(i18nKeys: I18nKeys) {

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
    _ ← $insert(nextR) doIf nextR.nonEmpty
    _ ← systemMessage(next, _.rematchOfferAccepted)
    prevWR ← WatcherRoomRepo room prev.id
    nextWR = prevWR.rematchCopy(next.id, nbMessagesCopiedToRematch)
    _ ← $insert(nextWR) doIf nextWR.nonEmpty
  } yield ()

  def playerMessage(ref: PovRef, text: String): Fu[List[Event.Message]] =
    cleanupText(text) zmap { t ⇒
      RoomRepo.addMessage(ref.gameId, ref.color.name, t) inject {
        Event.Message(ref.color.name, t) :: Nil
      }
    }

  def watcherMessage(
    gameId: String,
    userId: Option[String],
    text: String): Fu[List[Event.WatcherMessage]] =
    cleanupText(text) zmap { t ⇒
      WatcherRoomRepo.addMessage(gameId, userId, t) inject {
        Event.WatcherMessage(userId, text) :: Nil
      }
    }

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

  private def cleanupText(text: String) = {
    val cleanedUp = text.trim.replace(""""""", "'")
    (cleanedUp.size <= 140 && cleanedUp.nonEmpty) option cleanedUp
  }

  private def messageToEn(message: SelectI18nKey): String =
    message(i18nKeys).en()
}
