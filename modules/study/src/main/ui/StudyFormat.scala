package lila.study
package ui

import play.api.mvc.RequestHeader

import lila.core.security.LilaCookie
import lila.common.HTTPRequest
import lila.ui.Context

enum StudyFormat:
  case card, compact, mobile
  def key = toString
  def toggle = if this == card then StudyFormat.compact else StudyFormat.card
  def compact = this == StudyFormat.compact

object StudyFormat:
  val byKey = values.mapBy(_.key)

final class StudyFormatStore(baker: LilaCookie):

  import StudyFormatStore.*

  def toggle(using req: RequestHeader) =
    val backUrl = HTTPRequest.referer(req) | routes.Study.allDefault().url
    val cookie = HTTPRequest.queryStringGet("format").flatMap(StudyFormat.byKey.get).map(write)
    cookie.foldLeft(play.api.mvc.Results.Redirect(backUrl))(_.withCookies(_))

  private def write(format: StudyFormat) = baker.cookie(
    name = name,
    value = format.key,
    maxAge = 31536000.some // one year
  )

object StudyFormatStore:

  private val name = "studyListFormat"

  private def read(using req: RequestHeader) =
    req.cookies.get(name).map(_.value).flatMap(StudyFormat.byKey.get)

  given (using ctx: Context): StudyFormat = read(using ctx.req) | {
    if HTTPRequest.acceptsJson(ctx.req) then StudyFormat.mobile else StudyFormat.card
  }
