package lila.chat

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lila.socket.Socket.makeMessage
import lila.socket.SocketMember
import lila.user.User

private final class Palantir {

  import Palantir._

  private val channels: Cache[Chat.Id, Channel] = Scaffeine()
    .expireAfterWrite(1 minute)
    .build[Chat.Id, Channel]

  def ping(chatId: Chat.Id, userId: User.ID, member: SocketMember): Unit = {
    val channel = channels.get(chatId, _ => new Channel)
    channel.add(userId, member)
    member.push(makeMessage("palantir", channel.userIds.filter(userId !=)))
    lila.mon.palantir.channels(channels.estimatedSize)
  }
}

private object Palantir {

  class Channel {

    private val members: Cache[User.ID, SocketMember] = Scaffeine()
      .expireAfterWrite(10 seconds)
      .build[User.ID, SocketMember]

    def add(uid: User.ID, member: SocketMember) = members.put(uid, member)

    def userIds = members.asMap.keys
  }

  case class Ping(chatId: Chat.Id, userId: User.ID, member: SocketMember)
}
