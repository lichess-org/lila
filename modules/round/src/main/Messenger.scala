package lila.round

import org.apache.commons.lang3.StringEscapeUtils.escapeXml

import chess.Color
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

  def init(game: Game): Fu[List[Event]] =
    systemMessage(game, game.creatorColor.fold(_.whiteCreatesTheGame, _.blackCreatesTheGame)) >>
    systemMessage(game, game.invitedColor.fold(_.whiteJoinsTheGame, _.blackJoinsTheGame))

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
    userOption ← userId.??(UserRepo.byId)
    message ← userOrAnonMessage(userOption, text).future
    (u, t) = message
    _ ← WatcherRoomRepo.addMessage(gameId, u, t)
  } yield Event.WatcherMessage(u, t) :: Nil

  def systemMessage(game: Game, message: SelectI18nKey): Fu[List[Event]] =
    game.hasChat ?? {
      trans(message) |> { messageKey ⇒
        RoomRepo.addSystemMessage(game.id, messageKey) map {
          _ ?? List(Event.Message("system", messageKey))
        }
      }
    }

  def toggleChat(ref: PovRef, status: Boolean): Fu[List[Event.Message]] =
    "%s chat is %s".format(
      ref.color.toString.capitalize,
      status.fold("en", "dis") + "abled"
    ) |> { message ⇒
        RoomRepo.addSystemMessage(ref.gameId, message) map {
          _ ?? List(Event.Message("system", message))
        }
      }

  private def trans(message: SelectI18nKey, args: Any*): String =
    message(i18nKeys).en(args: _*)
}
