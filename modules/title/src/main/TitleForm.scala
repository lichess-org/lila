package lila.title

import play.api.data.*
import play.api.data.Forms.*
import chess.FideId

import lila.common.Form.{ cleanNonEmptyText, playerTitle, fideId, into, url }
import lila.core.id.ImageId

final class TitleForm:

  val create = Form:
    mapping(
      "realName"              -> cleanNonEmptyText(minLength = 3, maxLength = 120),
      "title"                 -> playerTitle.field,
      "fideId"                -> optional(fideId.field),
      "nationalFederationUrl" -> optional(url.field),
      "idDocument"            -> text.into[ImageId],
      "selfie"                -> text.into[ImageId],
      "public"                -> boolean,
      "coach"                 -> boolean,
      "comment"               -> optional(cleanNonEmptyText)
    )(TitleRequest.FormData.apply)(unapply)

  def edit(data: TitleRequest.FormData) = create.fill(data)
