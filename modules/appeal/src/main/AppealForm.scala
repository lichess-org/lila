package lila.appeal

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ options, numberIn, cleanNonEmptyText }

object AppealForm:

  val untilMonths = options(List(1, 3, 6, 12), "%d month{s}")
  val topicFilterChoices = ("all" :: AppealTopicApi.relevant.map(_.key)).map(t => t -> t)

  val sleep = Form:
    single("months" -> optional(numberIn(untilMonths)))

  val form = Form:
    single("text" -> cleanNonEmptyText(minLength = 2, maxLength = Appeal.maxLength))

  val modForm = Form:
    tuple(
      "text" -> cleanNonEmptyText,
      "close" -> optional(boolean)
    )
