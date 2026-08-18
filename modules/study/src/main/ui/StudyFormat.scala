package lila.study
package ui

import play.api.mvc.RequestHeader

import lila.core.security.LilaCookie

enum StudyFormat:
  case card, compact
  def key = toString
  def toggle = if this == card then StudyFormat.compact else StudyFormat.card
  def compact = this == StudyFormat.compact

object StudyFormat:
  val byKey = values.mapBy(_.key)

final class StudyFormatStore(baker: LilaCookie):

  import StudyFormatStore.*

  def write(format: StudyFormat) = baker.cookie(
    name = name,
    value = format.key,
    maxAge = 31536000.some // one year
  )

object StudyFormatStore:

  private val name = "studyListFormat"

  def read(using req: RequestHeader): StudyFormat =
    req.cookies.get(name).map(_.value).flatMap(StudyFormat.byKey.get) | StudyFormat.card

  given (using ctx: lila.ui.Context): StudyFormat = read(using ctx.req)
