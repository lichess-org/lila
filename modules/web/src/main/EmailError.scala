package lila.web

import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.Request

final class EmailError(cacheApi: lila.memo.CacheApi):

  private type ErrorMsg = String

  private val storage = cacheApi.notLoadingSync[EmailAddress, ErrorMsg](32, "email.error"):
    _.expireAfterAccess(1.day).build()

  private val form = Form:
    tuple(
      "email" -> nonEmptyText,
      "error" -> nonEmptyText
    )

  def setFromReq()(using Request[?], FormBinding) =
    for
      (emailStr, error) <- form.bindFromRequest().value
      email <- EmailAddress.from(emailStr)
    yield storage.put(email, error.take(800))

  def get(email: EmailAddress): Option[ErrorMsg] =
    storage.getIfPresent(email)
