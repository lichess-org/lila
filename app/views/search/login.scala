package views.search

import lila.app.templating.Environment.{ *, given }

object login:

  def apply(nbGames: Long)(using PageContext) =
    views.base.layout(
      title = trans.search.searchInXGames.txt(nbGames.localize, nbGames),
      moreCss = cssTag("search")
    ):
      main(cls := "box box-pad page-small search search-login")(
        h1(cls := "box__top")(trans.search.advancedSearch()),
        div(cls := "search__login")(
          p(a(href := routes.Auth.signup)(trans.site.youNeedAnAccountToDoThat()))
        )
      )
