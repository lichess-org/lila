package views.html.tutor

import scalatags.Text.tags2.abbr

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.insight.{ InsightDimension, InsightMetric, Phase }
import lila.tutor.TutorNumber

sealed class TutorConcept(val name: String, val description: String, val unit: TutorUnit)

object concept {

  import TutorUnit._

  val speed =
    new TutorConcept("Speed", "How fast you play, based on average remaining time on your clock.", percent)
  val clockFlagVictory =
    new TutorConcept("Flagging skills", "How often you win by flagging the opponent.", percent)
  val clockTimeUsage = new TutorConcept(
    "Clock time usage",
    "How well you make use of your available time. Losing games with a lot of time left is poor usage of the clock.",
    percent
  )

  val accuracy          = new TutorConcept("Accuracy", InsightMetric.MeanAccuracy.description, percent)
  val tacticalAwareness = new TutorConcept("Tactical Awareness", InsightMetric.Awareness.description, percent)

  val performance = new TutorConcept("Performance", InsightMetric.Performance.description, rating)

  def phase(phase: Phase, unit: TutorUnit = percent) = adhoc(phase.name, unit)

  def adhoc(name: String, unit: TutorUnit = percent) = new TutorConcept(name, "", unit)

  def show(concept: TutorConcept): Tag =
    span(cls := "tutor__concept")(concept.name)
}

sealed trait TutorUnit {
  def html[V: TutorNumber](v: V): Frag
  def text[V: TutorNumber](v: V): String
}

object TutorUnit {

  val rating = new TutorUnit {
    def html[V: TutorNumber](v: V)                     = strong(text(v))
    def text[V](v: V)(implicit number: TutorNumber[V]) = f"${number double v}%1.0f"
  }
  val percent = new TutorUnit {
    def html[V: TutorNumber](v: V)                          = frag(strong(number(v)), "%")
    def text[V: TutorNumber](v: V)                          = s"${number(v)}%"
    private def number[V](v: V)(implicit n: TutorNumber[V]) = f"${n double v}%1.1f"
  }
}
