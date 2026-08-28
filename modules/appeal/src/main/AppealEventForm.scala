package lila.appeal

import play.api.data.*
import play.api.data.Forms.*
import lila.common.Form.{ cleanNonEmptyText, into }

object AppealEventForm:

  val kindForm = Form(single("kind" -> nonEmptyText))

  case class ChoiceData(nodeId: NodeId, answerId: AnswerId)
  val choiceForm = Form(
    mapping(
      "nodeId" -> nonEmptyText.into[NodeId],
      "answerId" -> nonEmptyText.into[AnswerId]
    )(ChoiceData.apply)(unapply)
  )

  type MessageData = String
  val messageForm = Form(single("text" -> cleanNonEmptyText(minLength = 2, maxLength = Appeal.maxLength)))
