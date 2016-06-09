package lila.chat

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class TempBan(coll: Coll) {

  import Chat.userChatBSONHandler

  def add(chatId: ChatId, modId: String, userId: String): Funit =
    coll.byId[UserChat](chatId) zip UserRepo.named(modId) zip UserRepo.named(userId) flatMap {
      case ((Some(chat), Some(mod)), Some(user)) => add(chat, mod, user)
      case _                                   => fuccess(none)
    }

  def add(chat: UserChat, mod: User, user: User): Funit = ???
}
