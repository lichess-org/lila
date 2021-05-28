package lila.chat

import play.api.libs.json._

import lila.common.LightUser
import lila.common.Json._

object JsonView {

  import writers._

  lazy val timeoutReasons = Json toJson ChatTimeout.Reason.all

  def apply(chat: AnyChat): JsValue =
    chat match {
      case c: MixedChat => mixedChatWriter writes c
      case c: UserChat  => userChatWriter writes c
    }

  def apply(line: Line): JsObject = lineWriter writes line

  def userModInfo(u: UserModInfo)(implicit lightUser: LightUser.GetterSync) =
    lila.user.JsonView.modWrites.writes(u.user) ++ Json.obj(
      "history" -> u.history
    )

  def mobile(chat: AnyChat, writeable: Boolean = true) =
    Json.obj(
      "lines"     -> apply(chat),
      "writeable" -> writeable
    )

  object writers {

    implicit val chatIdWrites: Writes[Chat.Id] = stringIsoWriter(Chat.chatIdIso)

    implicit val timeoutReasonWriter: Writes[ChatTimeout.Reason] = OWrites[ChatTimeout.Reason] { r =>
      Json.obj("key" -> r.key, "name" -> r.name)
    }

    implicit def timeoutEntryWriter(implicit
        lightUser: LightUser.GetterSync
    ): OWrites[ChatTimeout.UserEntry] =
      OWrites[ChatTimeout.UserEntry] { e =>
        Json.obj(
          "reason" -> e.reason.key,
          "mod"    -> lightUser(e.mod).fold("?")(_.name),
          "date"   -> e.createdAt
        )
      }

    implicit val mixedChatWriter: Writes[MixedChat] = Writes[MixedChat] { c =>
      JsArray(c.lines map lineWriter.writes)
    }

    implicit val userChatWriter: Writes[UserChat] = Writes[UserChat] { c =>
      JsArray(c.lines map userLineWriter.writes)
    }

    implicit private[chat] val lineWriter: OWrites[Line] = OWrites[Line] {
      case l: UserLine   => userLineWriter writes l
      case l: PlayerLine => playerLineWriter writes l
    }

    implicit private val userLineWriter = OWrites[UserLine] { l =>
      Json
        .obj(
          "u" -> l.username,
          "t" -> l.text
        )
        .add("r" -> l.troll)
        .add("d" -> l.deleted)
        .add("p" -> l.patron)
        .add("title" -> l.title)
    }

    implicit private val playerLineWriter = OWrites[PlayerLine] { l =>
      Json.obj(
        "c" -> l.color.name,
        "t" -> l.text
      )
    }
  }
}
