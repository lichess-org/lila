package lila.title

import play.api.data.*
import play.api.data.Forms.*
import chess.FideId

import lila.common.Form.{ cleanNonEmptyText, playerTitle, fideId, into, url }
import lila.core.id.ImageId

final class TitleForm:

  val create = Form:
    mapping(
      "realName"      -> cleanNonEmptyText(minLength = 3, maxLength = 120),
      "title"         -> playerTitle.field,
      "fideId"        -> optional(fideId.field),
      "federationUrl" -> optional(url.field),
      "public"        -> boolean,
      "coach"         -> boolean,
      "comment"       -> optional(cleanNonEmptyText(maxLength = 2000))
    )(TitleRequest.FormData.apply)(unapply)
      .verifying(
        "Missing FIDE ID or federation URL.",
        d => d.fideId.isDefined || d.federationUrl.isDefined
      )
      .verifying(
        "The coach profile requires a public title.",
        d => !d.coach || d.public
      )

  def edit(data: TitleRequest.FormData) = create.fill(data)
