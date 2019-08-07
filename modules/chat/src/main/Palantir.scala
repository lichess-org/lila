package lila.chat

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lila.socket.Socket.makeMessage
import lila.socket.SocketMember
import lila.user.User

private final class Palantir {

  import Palantir._

  private val stones: Cache[Chat.Id, Stone] = Scaffeine()
    .expireAfterWrite(1 minute)
    .build[Chat.Id, Stone]

  def toggle(chatId: Chat.Id, userId: User.ID, member: SocketMember, on: Boolean): Unit = {
    val stone = stones.getIfPresent(chatId).getOrElse(emptyStone) |> { stone =>
      if (on) stone.add(userId, member)
      else stone remove userId
    }
    stones.put(chatId, stone)
    member.push(makeMessage("palantir", stone.userIds.filter(userId !=)))
  }
}

private object Palantir {

  case class Stone(members: Map[User.ID, SocketMember]) {
    def add(uid: User.ID, member: SocketMember) = copy(
      members = members + (uid -> member)
    )
    def remove(uid: User.ID) = copy(
      members = members - uid
    )
    def userIds = members.keys.toList
  }

  val emptyStone = Stone(Map.empty)

  case class Toggle(chatId: Chat.Id, userId: User.ID, member: SocketMember, on: Boolean)
}
