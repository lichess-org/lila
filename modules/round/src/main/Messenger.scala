package lila.round

import lila.chat.{ ChatApi, ChatTimeout }
import lila.core.shutup.PublicSource

final class Messenger(api: ChatApi):

  def system(game: Game, message: String): Unit =
    system(persistent = true)(game, message)

  def volatile(game: Game, message: String): Unit =
    system(persistent = false)(game, message)

  def apply(game: Game, message: Messenger.SystemMessage): Unit = message match
    case Messenger.SystemMessage.Persistent(msg) => system(persistent = true)(game, msg)
    case Messenger.SystemMessage.Volatile(msg)   => system(persistent = false)(game, msg)

  def system(persistent: Boolean)(game: Game, message: String): Unit = if game.nonAi then
    api.userChat.volatile(chatWatcherId(game.id.into(ChatId)), message, _.round)
    if persistent then api.userChat.system(game.id.into(ChatId), message, _.round)
    else api.userChat.volatile(game.id.into(ChatId), message, _.round)

  def watcher(gameId: GameId, userId: UserId, text: String) =
    api.userChat.write(gameWatcherId(gameId), userId, text, PublicSource.Watcher(gameId).some, _.round)

  private val whisperCommands = List("/whisper ", "/w ", "/W ")

  def owner(gameId: GameId, userId: UserId, text: String): Funit =
    whisperCommands
      .collectFirst:
        case command if text.startsWith(command) =>
          val source = PublicSource.Watcher(gameId)
          api.userChat.write(gameWatcherId(gameId), userId, text.drop(command.length), source.some, _.round)
      .getOrElse:
        (!text.startsWith("/")).so: // mistyped command?
          api.userChat.write(gameId.into(ChatId), userId, text, publicSource = none, _.round)

  def owner(game: Game, anonColor: Color, text: String): Funit =
    (game.sourceIs(_.Friend) || presets.contains(text))
      .so(api.playerChat.write(game.id.into(ChatId), anonColor, text, _.round))

  def timeout(chatId: ChatId, suspect: UserId, reason: String, text: String)(using mod: MyId): Funit =
    ChatTimeout
      .Reason(reason)
      .so: r =>
        api.userChat.timeout(chatId, suspect, r, ChatTimeout.Scope.Global, text, _.round)

  private val presets = Set(
    "Hello",
    "Good luck",
    "Have fun!",
    "You too!",
    "Good game",
    "Well played",
    "Thank you",
    "I've got to go",
    "Bye!"
  )

  private def chatWatcherId(chatId: ChatId) = ChatId(s"$chatId/w")
  private def gameWatcherId(gameId: GameId) = ChatId(s"$gameId/w")

private object Messenger:

  enum SystemMessage:
    val msg: String
    case Persistent(msg: String)
    case Volatile(msg: String)
