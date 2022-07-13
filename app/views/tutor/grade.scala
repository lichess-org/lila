package views.html.tutor

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._
import scalatags.Text

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.insight.InsightPosition
import lila.tutor.{
  Rating,
  TutorBothValueOptions,
  TutorBothValuesAvailable,
  TutorFullReport,
  TutorNumber,
  ValueCount
}

object grade {

  def peerGrade[A](
      c: TutorConcept,
      metricOptions: TutorBothValueOptions[A],
      titleTag: Text.Tag = h3
  )(implicit lang: Lang, number: TutorNumber[A]): Option[Tag] =
    metricOptions.asAvailable map { metric =>
      div(cls := "tutor-grade")(
        titleTag(cls := "tutor-grade__name")(concept show c),
        gradeVisual(c, metric)
      )
    }

  def peerGradeWithDetail[A: TutorNumber](
      c: TutorConcept,
      metricOptions: TutorBothValueOptions[A],
      position: InsightPosition,
      titleTag: Text.Tag = h2
  )(implicit lang: Lang): Option[Tag] =
    metricOptions.asAvailable map { metric =>
      div(cls := "tutor-grade tutor-grade--detail")(
        titleTag(cls := "tutor-grade__name")(concept show c),
        c.description.nonEmpty option p(cls := "tutor-grade__concept")(c.description),
        gradeVisual(c, metric),
        div(cls := "tutor-grade__detail")(
          c.unit.html(metric.mine.value),
          em(title := s"${metric.peer.count} peer ${position.short}")(
            " vs ",
            c.unit.html(metric.peer.value),
            " (peers)"
          ),
          " over ",
          metric.mine.count.localize,
          " ",
          position.short,
          !metric.mine.reliableEnough option frag(
            " (",
            em(cls := "text", dataIcon := "")("small sample!"),
            ")"
          )
        )
      )
    }

  private def gradeVisual[A](c: TutorConcept, metric: TutorBothValuesAvailable[A])(implicit
      number: TutorNumber[A]
  ) = {
    val grade = metric.grade
    div(
      cls   := s"tutor-grade__visual tutor-grade__visual--${grade.wording.id}",
      title := s"${c.unit.text(metric.mine.value)} vs peers: ${c.unit.text(metric.peer.value)}"
    )(
      lila.tutor.Grade.Wording.list.map { gw =>
        div(cls := (grade.wording >= gw).option("lit"))
      }
    )
  }
}
