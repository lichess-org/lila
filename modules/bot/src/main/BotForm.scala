package lila.bot

import play.api.data._
import play.api.data.Forms._

object BotForm {

  val chat = Form(
    mapping(
      "text" -> nonEmptyText(maxLength = lila.chat.Line.textMaxSize),
      "room" -> text.verifying(Set("player", "spectator") contains _)
    )(ChatData.apply)(ChatData.unapply)
  )

  case class ChatData(
      text: String,
      room: String
  )
}
