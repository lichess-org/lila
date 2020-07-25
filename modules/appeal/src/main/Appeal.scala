package lila.appeal

import lila.user.User
import org.joda.time.DateTime

case class Appeal(
  _id: User.ID,
  msgs: List[AppealMsg],
  status: Appeal.Status,
  createdAt: DateTime,
  updatedAt: DateTime
)

object Appeal {

  sealed trait Status {
    val key = toString.toLowerCase
  }
  object Status {
    case object Unread extends Status
    case object Read extends Status
    case object Closed extends Status
    case object Muted extends Status
    val all = List[Status](Unread, Read, Closed, Muted)
    def apply(key: String) = all.find(_.key == key)
  }
  
}

case class AppealMsg(
  by: User.ID,
  text: String,
  at: DateTime
)
