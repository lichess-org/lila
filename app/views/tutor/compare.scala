package views.html.tutor

import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.insight.{ InsightDimension, Phase }
import lila.rating.PerfType
import lila.tutor.{ Grade, TutorCompare, TutorMetric }

private object compare {

  def show(comp: TutorCompare.AnyComparison)(implicit lang: Lang) = showWithPerf(comp, none)

  def showWithPerf(comp: TutorCompare.AnyComparison, perf: Option[PerfType] = None)(implicit lang: Lang) =
    li(
      "Your ",
      perf.map(p => frag(p.trans, " ")),
      showMetric(comp),
      " in the ",
      strong(showDimension(comp.dimension)),
      " is ",
      showQuality(comp.grade),
      " ",
      comp.reference match {
        case TutorCompare.DimAvg(_) => frag("in ", otherDims(comp.dimensionType))
        case TutorCompare.Peers(_)  => frag("your peers'")
      },
      "."
    )

  private def otherDims[D](dimension: InsightDimension[D]) = dimension match {
    case InsightDimension.Phase         => "other phases"
    case InsightDimension.OpeningFamily => "other openings"
    case _                              => "others"
  }

  private[tutor] def showQuality(quality: Grade) =
    (if (quality.better) goodTag else if (quality.worse) badTag else span)(quality.wording.value)

  private[tutor] def showMetric(comp: TutorCompare.AnyComparison): String =
    (comp.metric match {
      case TutorMetric.GlobalClock => "global speed"
      case TutorMetric.ClockUsage  => "clock usage"
      case metric                  => metric.metric.name.toLowerCase
    })

  private[tutor] def showDimension[D](dimension: D): String = dimension match {
    case d: LilaOpeningFamily => d.name.value
    case d: Phase             => d.name.toLowerCase
    case d                    => d.toString.toLowerCase
  }
}
