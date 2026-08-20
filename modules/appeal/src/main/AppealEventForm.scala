package lila.appeal

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ options, numberIn, cleanNonEmptyText, cleanText }

object AppealEventForm:

  val kindForm = Form(single("kind" -> nonEmptyText))
