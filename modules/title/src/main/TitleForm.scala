package lila.title

import chess.FideId
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanNonEmptyText, fideId, playerTitle, stringIn, url }

object TitleForm:

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

  val process = Form:
    mapping(
      "text"   -> optional(nonEmptyText(maxLength = 2000)),
      "action" -> stringIn(Set("approve", "reject", "feedback"))
    )(ProcessData.apply)(unapply)

  case class ProcessData(text: Option[String], action: String):
    def status =
      import TitleRequest.Status
      action match
        case "approve" => Status.approved
        case "reject"  => Status.rejected
        case _         => Status.feedback(text | "?")
