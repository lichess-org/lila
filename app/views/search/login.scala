package views.html.search

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object login {

  def apply(nbGames: Long)(implicit ctx: Context) = {
    views.html.base.layout(
      title = trans.search.searchInXGames.txt(nbGames.localize, nbGames),
      moreCss = cssTag("search")
    ) {
      main(cls := "box box-pad page-small search search-login")(
        h1(trans.search.advancedSearch()),
        div(cls := "search__login")(
          p(a(href := routes.Auth.signup())(trans.youNeedAnAccountToDoThat()))
        )
      )
    }
  }
}
