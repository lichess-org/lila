package views.html.tutor

import lila.insight.InsightDimension
import lila.insight.InsightMetric

sealed class TutorConcept(val name: String, val description: String)

object concept {

  val Speed = new TutorConcept("Speed", "How fast you play, based on average remaining time on your clock.")
  val ClockFlagVictory = new TutorConcept("Clock flag victory", "How often you win by flagging the opponent.")
  val ClockTimeUsage = new TutorConcept(
    "Clock time usage",
    "How well you make use of your available time. Losing games with a lot of time left is poor usage of the clock."
  )

  val Accuracy          = new TutorConcept("Accuracy", InsightMetric.MeanAccuracy.description)
  val TacticalAwareness = new TutorConcept("Tactical Awareness", InsightMetric.Awareness.description)
}
