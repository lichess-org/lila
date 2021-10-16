package views.html.relation

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.FavoriteOpponents
import lila.relation.Related
import lila.user.User

object bits {

  def friends(u: User, pag: Paginator[Related])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.friends.txt()}")(
      h1(
        a(href := routes.User.show(u.username), dataIcon := "", cls := "text"),
        trans.friends()
      ),
      pagTable(pag, routes.Relation.following(u.username))
    )

  def blocks(u: User, pag: Paginator[Related])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.blocks.pluralSameTxt(pag.nbResults)}")(
      div(cls := "box__top")(
        h1(userLink(u, withOnline = false)),
        div(cls := "actions")(
          trans.blocks.pluralSame(pag.nbResults)
        )
      ),
      pagTable(pag, routes.Relation.blocks())
    )

  def opponents(u: User, sugs: List[lila.relation.Related])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.favoriteOpponents.txt()}")(
      h1(
        a(href := routes.User.show(u.username), dataIcon := "", cls := "text"),
        trans.favoriteOpponents(),
        " (",
        trans.nbGames.pluralSame(FavoriteOpponents.gameLimit),
        ")"
      ),
      table(cls := "slist")(
        tbody(
          if (sugs.nonEmpty) sugs.map { r =>
            tr(
              td(userLink(r.user)),
              td(showBestPerf(r.user)),
              td(
                r.nbGames.filter(_ > 0).map { nbGames =>
                  a(href := s"${routes.User.games(u.username, "search")}?players.b=${r.user.username}")(
                    trans.nbGames.plural(nbGames, nbGames.localize)
                  )
                }
              ),
              td(
                views.html.relation.actions(r.user.id, r.relation, followable = r.followable, blocked = false)
              )
            )
          }
          else tr(td(trans.none()))
        )
      )
    )

  def layout(title: String)(content: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relation"),
      moreJs = infiniteScrollTag
    ) {
      main(cls := "box page-small")(content)
    }

  private def pagTable(pager: Paginator[Related], call: Call)(implicit ctx: Context) =
    table(cls := "slist")(
      if (pager.nbResults > 0)
        tbody(cls := "infinite-scroll")(
          pager.currentPageResults.map { r =>
            tr(cls := "paginated")(
              td(userLink(r.user)),
              td(showBestPerf(r.user)),
              td(trans.nbGames.plural(r.user.count.game, r.user.count.game.localize)),
              td(actions(r.user.id, relation = r.relation, followable = r.followable, blocked = false))
            )
          },
          pagerNextTable(pager, np => addQueryParameter(call.url, "page", np))
        )
      else tbody(tr(td(colspan := 2)(trans.none())))
    )
}
