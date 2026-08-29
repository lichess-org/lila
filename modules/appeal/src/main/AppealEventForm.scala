package lila.appeal

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import lila.common.Form.{ cleanNonEmptyText, into, typeIn, formatter }

object AppealEventForm:

  private given Formatter[AppealMsg.Kind] =
    formatter.stringOptionFormatter[AppealMsg.Kind](_.key, AppealMsg.Kind.apply)
  val kindForm = Form(single("kind" -> typeIn[AppealMsg.Kind](AppealMsg.Kind.values.toSet)))

  case class ChoiceData(nodeId: NodeId, answerId: AnswerId)
  val choiceForm = Form(
    mapping(
      "nodeId" -> nonEmptyText.into[NodeId],
      "answerId" -> nonEmptyText.into[AnswerId]
    )(ChoiceData.apply)(unapply)
  )

  type MessageData = String
  val messageForm = Form(single("text" -> cleanNonEmptyText(minLength = 2, maxLength = Appeal.maxLength)))
