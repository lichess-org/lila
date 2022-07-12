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
        gradeVisual(metric)
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
        gradeVisual(metric),
        div(cls := "tutor-grade__detail")(
          c.unit.render(metric.mine.value),
          em(title := s"${metric.peer.count} peer ${position.short}")(
            " vs ",
            c.unit.render(metric.peer.value),
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

  private def gradeVisual[A](metric: TutorBothValuesAvailable[A])(implicit number: TutorNumber[A]) = {
    val grade       = metric.grade
    val minePercent = bits.percentNumber(metric.mine.value)
    val peerPercent = bits.percentNumber(metric.peer.value)
    div(
      cls   := s"tutor-grade__visual tutor-grade__visual--${grade.wording.id}",
      title := s"$minePercent% vs $peerPercent%"
    )(
      lila.tutor.Grade.Wording.list.map { gw =>
        div(cls := (grade.wording >= gw).option("lit"))
      }
    )
  }
}
