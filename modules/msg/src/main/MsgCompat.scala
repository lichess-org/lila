package lila.msg

import play.api.data.*
import play.api.data.Forms.*

final class MsgCompat(api: MsgApi):

  private val replyForm = Form(single("text" -> text(minLength = 3)))

  def reply(
      userId: UserId
  )(using play.api.mvc.Request[?], FormBinding)(using me: Me): Either[Form[?], Funit] =
    replyForm
      .bindFromRequest()
      .fold(
        err => Left(err),
        text => Right(api.post(me, userId, text).void)
      )
