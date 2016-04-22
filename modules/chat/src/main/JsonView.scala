package lila.chat

import play.api.libs.json._

object JsonView {

  def apply(chat: AnyChat): JsValue = chat match {
    case c: MixedChat => mixedChatWriter writes c
    case c: UserChat  => userChatWriter writes c
  }

  def apply(line: Line): JsValue = lineWriter writes line

  implicit val mixedChatWriter: Writes[MixedChat] = Writes[MixedChat] { c =>
    JsArray(c.lines map lineWriter.writes)
  }

  implicit val userChatWriter: Writes[UserChat] = Writes[UserChat] { c =>
    JsArray(c.lines map userLineWriter.writes)
  }

  private[chat] implicit val lineWriter: Writes[Line] = Writes[Line] {
    case l: UserLine   => userLineWriter writes l
    case l: PlayerLine => playerLineWriter writes l
  }

  private implicit val userLineWriter = Writes[UserLine] { l =>
    Json.obj(
      "u" -> l.username,
      "t" -> l.text,
      "r" -> l.troll)
  }

  private implicit val playerLineWriter = Writes[PlayerLine] { l =>
    Json.obj(
      "c" -> l.color.name,
      "t" -> l.text)
  }
}
