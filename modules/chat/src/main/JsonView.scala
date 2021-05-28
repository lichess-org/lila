package lila.chat

import play.api.libs.json._

import lila.common.Json._
import lila.common.LightUser
import lila.user.User

final class JsonView(lightUserSync: LightUser.GetterSync) {

  import JsonView._

  def apply(chat: AnyChat): JsValue =
    chat match {
      case c: MixedChat => mixedChatWriter writes c
      case c: UserChat  => userChatWriter writes c
    }

  def userModInfo(u: UserModInfo)(implicit lightUser: LightUser.GetterSync) =
    lila.user.JsonView.modWrites.writes(u.user) ++ Json.obj(
      "history" -> u.history
    )

  def mobile(chat: AnyChat, writeable: Boolean = true) =
    Json.obj(
      "lines"     -> apply(chat),
      "writeable" -> writeable
    )

  implicit val mixedChatWriter: Writes[MixedChat] = Writes[MixedChat] { c =>
    JsArray(c.lines map lineWriter.writes)
  }

  implicit val userChatWriter: Writes[UserChat] = Writes[UserChat] { c =>
    JsArray(c.lines map userLineWriter.writes)
  }

  implicit val lineWriter: OWrites[Line] = OWrites[Line] {
    case l: UserLine   => userLineWriter writes l
    case l: PlayerLine => playerLineWriter writes l
  }

  implicit private val userLineWriter = OWrites[UserLine] { l =>
    val userId = User normalize l.username
    val u      = lightUserSync(userId) | LightUser.fallback(userId)
    Json
      .obj(
        "u" -> l.username,
        "t" -> l.text
      )
      .add("r" -> l.troll)
      .add("d" -> l.deleted)
      .add("title" -> u.title)
      .add("p" -> u.isPatron)
  }

  implicit def timeoutEntryWriter: OWrites[ChatTimeout.UserEntry] =
    OWrites[ChatTimeout.UserEntry] { e =>
      Json.obj(
        "reason" -> e.reason.key,
        "mod"    -> lightUserSync(e.mod).fold("?")(_.name),
        "date"   -> e.createdAt
      )
    }
}

object JsonView {

  lazy val timeoutReasons = Json toJson ChatTimeout.Reason.all

  implicit val chatIdWrites: Writes[Chat.Id] = stringIsoWriter(Chat.chatIdIso)

  implicit val timeoutReasonWriter: Writes[ChatTimeout.Reason] = OWrites[ChatTimeout.Reason] { r =>
    Json.obj("key" -> r.key, "name" -> r.name)
  }

  implicit private val playerLineWriter = OWrites[PlayerLine] { l =>
    Json.obj(
      "c" -> l.color.name,
      "t" -> l.text
    )
  }
}
