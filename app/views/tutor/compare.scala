package views.html.tutor

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.insight.Phase
import lila.tutor.{ RelativeQuality, TutorCompare }

private object compare {

  def show(comp: TutorCompare.Comparison[_, _]) =
    p(
      "Your ",
      comp.metricType.toString,
      " in the ",
      showDimension(comp.dimension),
      " is ",
      showQuality(comp.quality),
      " than ",
      comp.reference match {
        case TutorCompare.OtherDim(dim, _) => frag("in the ", dim.toString)
        case TutorCompare.Peers(_)         => frag("your peers'")
      }
    )

  private[tutor] def showQuality(quality: RelativeQuality) =
    (if (quality.positive) goodTag else badTag)(quality.wording.value)

  private[tutor] def showDimension[D](dimension: D): String = dimension match {
    case d: LilaOpeningFamily => d.name.value
    case d: Phase             => d.name
    case d                    => d.toString
  }
}
