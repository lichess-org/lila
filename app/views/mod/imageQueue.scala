package views.mod

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.memo.PicfitImage
import lila.report.ui.PendingCounts
import lila.report.Room.Scores

lazy val imageQueueUi = lila.mod.ui.ImageQueueUi(helpers)

object imageQueue:

  def show(
      images: Paginator[PicfitImage],
      scores: Scores,
      pending: PendingCounts
  )(using Context, Me) =
    views.report.ui.list.layout("image", scores, pending, Seq("mod.imageQueue"), infiniteScrollEsmInit)(
      views.mod.ui.reportMenu
    ):
      imageQueueUi.show(images)
