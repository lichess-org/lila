package lila.study
package ui

import play.api.mvc.RequestHeader

import lila.core.security.LilaCookie

enum StudyFormat:
  case card, compact
  def key = toString
  def toggle = if this == card then compact else card

object StudyFormat:
  val byKey = values.mapBy(_.key)

final class StudyFormatStore(baker: LilaCookie):

  import StudyFormatStore.*

  def write(format: StudyFormat) = baker.cookie(
    name = cookie.name,
    value = format.key,
    maxAge = 31536000.some // one year
  )

object StudyFormatStore:

  private[study] object cookie:
    val name = "studyListFormat"
    val maxAge = 31536000 // one year
    val valueSep = '/'
    val fieldSep = '!'

  def read(using req: RequestHeader): StudyFormat =
    req.cookies.get(cookie.name).map(_.value).flatMap(StudyFormat.byKey.get) | StudyFormat.card

  given (using ctx: lila.ui.Context): StudyFormat = read(using ctx.req)
