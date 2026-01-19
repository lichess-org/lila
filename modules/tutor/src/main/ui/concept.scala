package lila.tutor
package ui

import lila.insight.{ InsightMetric, Phase }
import lila.ui.ScalatagsTemplate.*
import lila.common.LilaOpeningFamily

sealed class TutorConcept(
    val name: String,
    val description: String,
    val unit: TutorUnit,
    val icon: TutorIcon = TutorIcon.bullseye
)

object concept:

  import TutorUnit.*

  val speed = TutorConcept(
    "Speed",
    "How fast you play, based on average remaining time on your clock.",
    percent,
    TutorIcon.sprint
  )
  val clockFlagVictory = TutorConcept(
    "Flagging skills",
    "How often you win by flagging the opponent.",
    percent,
    TutorIcon.flyingFlag
  )
  val clockTimeUsage = TutorConcept(
    "Clock time usage",
    "How well you make use of your available time. Losing games with a lot of time left is poor usage of the clock.",
    percent,
    TutorIcon.clockwork
  )

  val accuracy = TutorConcept("Accuracy", InsightMetric.MeanAccuracy.description, percent, TutorIcon.bullseye)
  val tacticalAwareness =
    TutorConcept("Tactical Awareness", InsightMetric.Awareness.description, percent, TutorIcon.eyeTarget)
  val resourcefulness = TutorConcept("Resourcefulness", "Come back from lost positions", percent)
  val conversion =
    TutorConcept("Conversion", "Convert good positions into victories", percent, TutorIcon.cycle)

  val performance =
    TutorConcept("Performance", InsightMetric.Performance.description, rating, TutorIcon.steelwingEmblem)

  private val phaseIcon: Phase => TutorIcon =
    case Phase.Opening => TutorIcon.groundSprout
    case Phase.Middle => TutorIcon.magicTrick
    case Phase.End => TutorIcon.towerFall

  def phase(phase: Phase, unit: TutorUnit = percent) = adhoc(phase.name, phaseIcon(phase), unit)

  def opening(fam: LilaOpeningFamily, color: Color) = adhoc(s"${fam.name} as $color", TutorIcon.spellBook)

  private def adhoc(name: String, icon: TutorIcon, unit: TutorUnit = percent) =
    TutorConcept(name, "", unit, icon)

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
