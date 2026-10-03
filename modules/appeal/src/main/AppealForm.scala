package lila.appeal

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ options, numberIn, cleanNonEmptyText, cleanText }

object AppealForm:

  val untilMonths = options(List(1, 2, 3, 4, 6, 9, 12, 15, 24, 36), "%d month{s}")
  val topicFilterChoices =
    ("all" :: (AppealTopicApi.relevant :+ AppealTopic.chat).map(_.key)).map(t => t -> t)

  val sleep = Form:
    single("months" -> optional(numberIn(untilMonths)))

  case class Data(text: String, accounts: Option[AccountsDisclosure] = None)

  private val accountsMapping: Mapping[AccountsDisclosure] =
    mapping(
      "otherUsernames" -> optional(cleanText(maxLength = 500)),
      "moreForgotten" -> boolean,
      "household" -> optional(cleanText(maxLength = 500))
    )(AccountsDisclosure.apply)(unapply)

  val form = Form:
    mapping(
      "text" -> cleanNonEmptyText(minLength = 2, maxLength = Appeal.maxLength),
      "accounts" -> optional(accountsMapping)
    )(Data.apply)(unapply)

  val modForm = Form:
    tuple(
      "text" -> cleanNonEmptyText,
      "close" -> optional(boolean),
      "dismiss" -> optional(boolean)
    )
