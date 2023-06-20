package lila.msg

import lila.user.{ Me, User }
import lila.common.LightUser

case class MsgThread(
    id: MsgThread.Id,
    user1: UserId,
    user2: UserId,
    lastMsg: Msg.Last,
    del: Option[List[UserId]] = None,
    maskFor: Option[UserId] = None,
    maskWith: Option[Msg.Last] = None
):

  def users = List(user1, user2)

  def other(userId: UserId): UserId  = if (user1 == userId) user2 else user1
  def other(using me: Me): UserId    = other(me.userId)
  def other(user: LightUser): UserId = other(user.id)

  def delBy(userId: UserId) = del.exists(_ contains userId)

  def isPriority =
    !lastMsg.read && {
      user1 == User.lichessId || user2 == User.lichessId
    }

object MsgThread:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  case class WithMsgs(thread: MsgThread, msgs: List[Msg])

  case class WithContact(thread: MsgThread, contact: LightUser)

  case class Unread(thread: MsgThread)

  val idSep = '/'

  def id(u1: UserId, u2: UserId): Id = Id {
    sortUsers(u1, u2) match
      case (user1, user2) => s"$user1$idSep$user2"
  }

  def make(u1: UserId, u2: UserId, msg: Msg, maskFor: Option[UserId], maskWith: Option[Msg.Last]): MsgThread =
    sortUsers(u1, u2) match
      case (user1, user2) =>
        MsgThread(
          id = id(user1, user2),
          user1 = user1,
          user2 = user2,
          lastMsg = msg.asLast,
          maskFor = maskFor,
          maskWith = maskWith
        )

  private def sortUsers(u1: UserId, u2: UserId): (UserId, UserId) =
    if (u1.value < u2.value) (u1, u2) else (u2, u1)
