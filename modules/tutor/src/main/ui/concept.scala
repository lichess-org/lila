package lila.tutor
package ui

import lila.insight.{ InsightMetric, Phase }
import lila.ui.ScalatagsTemplate.*

sealed class TutorConcept(val name: String, val description: String, val unit: TutorUnit)

object concept:

  import TutorUnit.*

  val speed =
    TutorConcept("Speed", "How fast you play, based on average remaining time on your clock.", percent)
  val clockFlagVictory =
    TutorConcept("Flagging skills", "How often you win by flagging the opponent.", percent)
  val clockTimeUsage = TutorConcept(
    "Clock time usage",
    "How well you make use of your available time. Losing games with a lot of time left is poor usage of the clock.",
    percent
  )

  val accuracy = TutorConcept("Accuracy", InsightMetric.MeanAccuracy.description, percent)
  val tacticalAwareness = TutorConcept("Tactical Awareness", InsightMetric.Awareness.description, percent)
  val resourcefulness = TutorConcept("Resourcefulness", "Come back from lost positions", percent)
  val conversion = TutorConcept("Conversion", "Convert good positions into victories", percent)

  val performance = TutorConcept("Performance", InsightMetric.Performance.description, rating)

  def phase(phase: Phase, unit: TutorUnit = percent) = adhoc(phase.name, unit)

  def adhoc(name: String, unit: TutorUnit = percent) = TutorConcept(name, "", unit)

  def show(concept: TutorConcept): Tag =
    span(cls := "tutor__concept")(concept.name)

sealed trait TutorUnit:
  def html[V: TutorNumber](v: V): Frag
  def text[V: TutorNumber](v: V): String

object TutorUnit:

  val rating: TutorUnit = new:
    def html[V: TutorNumber](v: V) = strong(text(v))
    def text[V](v: V)(using number: TutorNumber[V]) = f"${number.double(v)}%1.0f"
  val percent: TutorUnit = new:
    def html[V: TutorNumber](v: V) = frag(strong(number(v)), "%")
    def text[V: TutorNumber](v: V) = s"${number(v)}%"
    private def number[V](v: V)(using n: TutorNumber[V]) = f"${n.double(v)}%1.1f"
