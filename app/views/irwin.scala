package views.irwin

import lila.app.templating.Environment.{ *, given }

import lila.game.GameExt.{ perfType, playerBlurPercent }

val ui = lila.irwin.IrwinUi(helpers)(
  playerBlurPercent = pov => pov.game.playerBlurPercent(pov.color)
)

def dashboard(dashboard: lila.irwin.IrwinReport.Dashboard)(using PageContext) =
  views.base.layout(
    title = "Irwin dashboard",
    moreCss = cssTag("mod.misc")
  ):
    main(cls := "page-menu")(
      views.mod.ui.menu("irwin"),
      ui.dashboard(dashboard)
    )

def kaladinDashboard(dashboard: lila.irwin.KaladinUser.Dashboard)(using PageContext) =
  views.base.layout(
    title = "Kaladin dashboard",
    moreCss = cssTag("mod.misc")
  ):
    main(cls := "page-menu")(
      views.mod.ui.menu("kaladin"),
      ui.kaladin.dashboard(dashboard)
    )
