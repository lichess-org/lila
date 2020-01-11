package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object download {

  def apply(user: lila.user.User)(implicit ctx: Context) = {
    val title = s"${user.username} • ${trans.exportGames.txt()}"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("form3")
    ) {
      main(cls := "box page-small search")(
        h1(title),
        form(cls := "form3 box__pad")(
          "The form here"
        )
      )
    }
  }
}
