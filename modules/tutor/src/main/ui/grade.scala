package lila.tutor
package ui
import scalatags.Text

import lila.core.i18n.Translate
import lila.insight.InsightPosition
import lila.ui.ScalatagsTemplate.{ *, given }

object grade:

  def peerGrade[A: TutorNumber](
      c: TutorConcept,
      metricOptions: TutorBothValueOptions[A],
      titleTag: Text.Tag = h3
  ): Option[Tag] =
    metricOptions.asAvailable.map: metric =>
      div(cls := s"tutor-grade tutor-grade--${metric.grade.wording.id}")(
        titleTag(cls := "tutor-grade__name")(c.icon.frag, concept.show(c)),
        gradeVisual(c, metric)
      )

  def peerGradeWithDetail[A: TutorNumber](
      c: TutorConcept,
      metricOptions: TutorBothValueOptions[A],
      position: InsightPosition,
      titleTag: Text.Tag = h2
  )(using Translate): Option[Tag] =
    metricOptions.asAvailable.map: metric =>
      div(cls := s"tutor-grade tutor-grade--${metric.grade.wording.id} tutor-grade--detail")(
        titleTag(cls := "tutor-grade__name")(c.icon.frag, concept.show(c)),
        c.description.nonEmpty.option(p(cls := "tutor-grade__concept")(c.description)),
        gradeVisual(c, metric),
        div(cls := "tutor-grade__detail")(
          c.unit.html(metric.mine.value),
          em(title := s"${metric.peer.count} peer ${position.short}")(
            " vs ",
            c.unit.html(metric.peer.value),
            " (peers)"
          ),
          " over ",
          lila.ui.NumberHelper.formatter.format(metric.mine.count),
          " ",
          position.short,
          (!metric.mine.reliableEnough).option(
            frag(
              " (",
              em(cls := "text", dataIcon := lila.ui.Icon.CautionTriangle)("small sample!"),
              ")"
            )
          )
        )
      )

  private def gradeVisual[A: TutorNumber](c: TutorConcept, metric: TutorBothValuesAvailable[A]) =
    val grade = metric.grade
    div(
      cls := s"tutor-grade__visual tutor-grade__visual--${grade.wording.id}",
      title := s"${c.unit.text(metric.mine.value)} vs peers: ${c.unit.text(metric.peer.value)}"
    )(
      lila.tutor.Grade.Wording.list.map { gw =>
        div(cls := (grade.wording >= gw).option("lit"))
      }
    )
