package lila.tutor
package ui

import java.time.LocalDate
import play.api.data.{ Form, Field }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.insight.MeanRating
import lila.memo.RateLimit

final class TutorReportsUi(helpers: Helpers, bits: TutorBits):
  import helpers.{ *, given }

  def newForm(user: UserId, form: Form[?], limit: RateLimit.Status)(using Context) =
    postForm(cls := "form3 tutor__report-form", action := routes.Tutor.compute(user.id)):
      form3.fieldset("Request a new Tutor report", toggle = form.globalError.isDefined.some)(
        form3.split(
          form3.group(form("from"), "Start date")(datePickr)(cls := "form-third"),
          form3.group(form("to"), "End date")(datePickr)(cls := "form-third"),
          div(cls := "form-group")(
            span(cls := "tutor__report-form__limit")(s"${limit.used} / ${limit.max} daily reports used."),
            form3.submit("Compute my tutor report", icon = none)(
              limit.reached.option(disabled),
              cls := limit.reached.option("disabled")
            )
          )
        ),
        form3.globalError(form)
      )

  def list(previews: List[TutorFullReport.Preview])(using Context) =
    div(cls := "tutor__reports-list")(previews.map(preview))

  private def preview(p: TutorFullReport.Preview)(using Context) =
    a(
      href := p.config.url.root,
      cls := List("tutor-preview" -> true, "tutor-preview--empty" -> p.perfs.isEmpty)
    )(
      span(cls := "tutor-preview__badges")(
        bits.reportTime(p.config),
        bits.reportMeta(p.stats.totalNbGames, p.stats.meanRating)
      ),
      if p.perfs.isEmpty then badTag(cls := "tutor-preview__empty")("Not enough games!")
      else
        span(cls := "tutor-preview__perfs"):
          p.perfs
            .take(3)
            .map: perf =>
              span(cls := "tutor-preview__perf", dataIcon := perf.perf.icon)(
                span(cls := "tutor-preview__perf__data")(
                  span(cls := "tutor-preview__perf__nb"):
                    trans.site.nbGames
                      .plural(perf.stats.totalNbGames, strong(perf.stats.totalNbGames.localize))
                  ,
                  span(cls := "tutor-preview__perf__rating")(
                    trans.site.rating(),
                    " ",
                    strong(perf.stats.rating)
                  )
                )
              )
    )

  private def datePickr(field: Field) = form3.flatpickr(
    field,
    withTime = false,
    local = true,
    minDate = TutorConfig.format(TutorConfig.minFrom).some,
    maxDate = TutorConfig.format(LocalDate.now).some
  )
