package lila.appeal

import play.api.data.*
import play.api.data.Forms.*

object AppealEventForm:

  val kindForm = Form(single("kind" -> nonEmptyText))

  case class ChoiceData(nodeId: String, answerId: String)
  val choiceForm = Form(
    mapping(
      "nodeId" -> nonEmptyText,
      "answerId" -> nonEmptyText
    )(ChoiceData.apply)(unapply)
  )

  type MessageData = String
  val messageForm = Form(single("text" -> nonEmptyText))
