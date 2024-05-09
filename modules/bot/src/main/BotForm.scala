package lila.bot

import play.api.data.*
import play.api.data.Forms.*

object BotForm:

  val chat: Form[ChatData] = Form(
    mapping(
      "text" -> nonEmptyText(maxLength = lila.chat.Line.textMaxSize),
      "room" -> text.verifying(Set("player", "spectator") contains _)
    )(ChatData.apply)(unapply)
  )

  case class ChatData(
      text: String,
      room: String
  )
