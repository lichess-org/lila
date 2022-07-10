package views.html.tutor

import scalatags.Text.tags2.abbr

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.insight.InsightDimension
import lila.insight.InsightMetric

sealed class TutorConcept(val name: String, val description: String)

object concept {

  val speed = new TutorConcept("Speed", "How fast you play, based on average remaining time on your clock.")
  val clockFlagVictory = new TutorConcept("Clock flag victory", "How often you win by flagging the opponent.")
  val clockTimeUsage = new TutorConcept(
    "Clock time usage",
    "How well you make use of your available time. Losing games with a lot of time left is poor usage of the clock."
  )

  val accuracy          = new TutorConcept("Accuracy", InsightMetric.MeanAccuracy.description)
  val tacticalAwareness = new TutorConcept("Tactical Awareness", InsightMetric.Awareness.description)

  def show(concept: TutorConcept): Tag =
    span(cls := "tutor__concept")(concept.name, iconTag("")(dataTitle := concept.description))

  def show(concept: String): Tag =
    span(cls := "tutor__concept")(concept)
}
