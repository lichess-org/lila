package views.html

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.game.GameExt.{ perfType, playerBlurPercent }

object irwin:

  val ui = lila.irwin.IrwinUi(i18nHelper, dateHelper, userHelper, gameHelper)(
    playerBlurPercent = pov => pov.game.playerBlurPercent(pov.color)
  )

  def dashboard(dashboard: lila.irwin.IrwinReport.Dashboard)(using PageContext) =
    views.html.base.layout(
      title = "Irwin dashboard",
      moreCss = cssTag("mod.misc")
    ):
      main(cls := "page-menu")(
        mod.menu("irwin"),
        ui.dashboard(dashboard)
      )

  def kaladinDashboard(dashboard: lila.irwin.KaladinUser.Dashboard)(using PageContext) =
    views.html.base.layout(
      title = "Kaladin dashboard",
      moreCss = cssTag("mod.misc")
    ):
      main(cls := "page-menu")(
        mod.menu("kaladin"),
        irwin.ui.kaladin.dashboard(dashboard)
      )
