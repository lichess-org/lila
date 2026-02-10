package lila.tutor
package ui

import lila.insight.{ InsightMetric, InsightDimension, Phase }
import lila.ui.ScalatagsTemplate.*
import lila.common.LilaOpeningFamily

sealed class TutorConcept(
    val name: String,
    val descShort: String,
    val descLong: Option[Frag],
    val unit: TutorUnit,
    val icon: TutorIcon,
    val insightPath: Option[String] = none
)

object concept:

  import TutorUnit.*

  val speed = TutorConcept(
    "Speed",
    "How fast you play, based on average remaining time on your clock.",
    frag(
      "Percentage of your available clock time during the middlegame and endgame phases. ",
      "Being consistently lower on time than your peers might indicate time pressure issues."
    ).some,
    percent,
    TutorIcon.sprint
  )
  val clockFlagVictory = TutorConcept(
    "Flagging skills",
    "How often you win by flagging the opponent.",
    frag(
      "Percentage of your victories that are won by flagging the opponent. ",
      "It might be higher if you favor time controls without an increment."
    ).some,
    percent,
    TutorIcon.flyingFlag
  )
  val clockTimeUsage = TutorConcept(
    "Clock time usage",
    "How well you make use of your available time.",
    frag(
      "Losing games with a lot of time left is poor usage of the clock. ",
      "This metric only looks at lost games, and the clock time you could have used to turn things around."
    ).some,
    percent,
    TutorIcon.clockwork
  )

  private val winPercentLink = a(href := "/page/accuracy#first-compute-win")
  private val accuracyLink = a(href := "/page/accuracy#then-compute-accuracy")
  private def insightPath(metric: InsightMetric, dimension: InsightDimension[?]) =
    routes.Insight.path(UserStr("me"), metric.key, dimension.key, "").url.some

  val accuracy =
    TutorConcept(
      "Accuracy",
      InsightMetric.MeanAccuracy.description,
      frag(
        "Accuracy is computed from engine evaluation and the ",
        winPercentLink("Lichess winning chances formula"),
        ". It applies to all your moves, and reflects the global objective quality of your play."
      ).some,
      percent,
      TutorIcon.targeting,
      insightPath(InsightMetric.MeanAccuracy, InsightDimension.Perf)
    )
  val tacticalAwareness =
    TutorConcept(
      "Tactical Awareness",
      InsightMetric.Awareness.description,
      frag(
        "The ",
        accuracyLink("accuracy"),
        " of your moves after your opponent blunders. It shows your ability to identify and take advantage of tactical opportunities."
      ).some,
      percent,
      TutorIcon.eyeTarget,
      insightPath(InsightMetric.Awareness, InsightDimension.Perf)
    )
  val resourcefulness =
    TutorConcept(
      "Resourcefulness",
      "Come back from lost positions",
      frag(
        "Percentage of games that you managed to save (win or draw) after being in a lost position (< 33% ",
        winPercentLink("winning chances"),
        ").",
        "It reflects how resilient you are in difficult positions, and how well you can find resources to turn things around."
      ).some,
      percent,
      TutorIcon.riposte
    )
  val conversion =
    TutorConcept(
      "Conversion",
      "Convert good positions into victories",
      frag(
        "When you get a winning position (",
        winPercentLink("winning chances"),
        " > 67%), how often do you convert it into a win? It reflects your ability to capitalize on advantages and close out games."
      ).some,
      percent,
      TutorIcon.cycle
    )

  val performance =
    TutorConcept(
      "Performance",
      InsightMetric.Performance.description,
      frag("Your Glicko2 rating if it was computed from these games only.").some,
      rating,
      TutorIcon.steelwingEmblem
    )

  val phaseIcon: Phase => TutorIcon =
    case Phase.Opening => TutorIcon.groundSprout
    case Phase.Middle => TutorIcon.magicTrick
    case Phase.End => TutorIcon.towerFall

  def phase(phase: Phase, unit: TutorUnit = percent) = adhoc(phase.name, phaseIcon(phase), unit)

  def opening(fam: LilaOpeningFamily, color: Color) = adhoc(s"${fam.name} as $color", TutorIcon.spellBook)

  private def adhoc(name: String, icon: TutorIcon, unit: TutorUnit = percent) =
    TutorConcept(name, "", none, unit, icon)

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
