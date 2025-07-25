package lila.web

import play.api.data.*
import play.api.data.Forms.*

object WebForms:

  val blind = Form:
    tuple(
      "enable" -> nonEmptyText,
      "redirect" -> nonEmptyText
    )
