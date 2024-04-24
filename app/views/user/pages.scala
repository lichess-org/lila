package views.html.user

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }

object download:

  lazy val ui = lila.user.ui.userGamesDownload(userHelper, i18nHelper, formHelper)

  def apply(user: User)(using ctx: PageContext): Frag =
    views.html.base.layout(
      title = s"${user.username} • ${trans.site.exportGames.txt()}",
      moreCss = cssTag("search"),
      modules = jsModule("bits.userGamesDownload")
    ):
      ui(user)
