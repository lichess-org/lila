package views.relation

import lila.app.UiEnv.{ *, given }

import scalalib.paginator.Paginator
import lila.game.FavoriteOpponents
import lila.relation.Related
import lila.core.perf.UserWithPerfs

object bits:

  def friends(u: User, pag: Paginator[Related[UserWithPerfs]])(using Context) =
    page(s"${u.username} • ${trans.site.friends.txt()}"):
      frag(
        boxTop(
          h1(
            a(href := routes.User.show(u.username), dataIcon := Icon.LessThan, cls := "text"),
            trans.site.friends()
          )
        ),
        pagTable(pag, routes.Relation.following(u.username))
      )

  def blocks(u: User, pag: Paginator[Related[UserWithPerfs]])(using Context) =
    page(s"${u.username} • ${trans.site.blocks.pluralSameTxt(pag.nbResults)}"):
      frag(
        boxTop(
          h1(userLink(u, withOnline = false)),
          div(cls := "actions")(trans.site.blocks.pluralSame(pag.nbResults))
        ),
        pagTable(pag, routes.Relation.blocks())
      )

  def opponents(u: User, sugs: List[Related[UserWithPerfs]])(using ctx: Context) =
    page(s"${u.username} • ${trans.site.favoriteOpponents.txt()}"):
      frag(
        boxTop:
          h1(
            a(href := routes.User.show(u.username), dataIcon := Icon.LessThan, cls := "text"),
            trans.site.favoriteOpponents(),
            " (",
            trans.site.nbGames.pluralSame(FavoriteOpponents.gameLimit),
            ")"
          )
        ,
        table(cls := "slist slist-pad"):
          tbody:
            if sugs.nonEmpty then
              sugs.map: r =>
                tr(
                  td(userLink(r.user)),
                  ctx.pref.showRatings.option(td(showBestPerf(r.user.perfs))),
                  td:
                    r.nbGames.filter(_ > 0).map { nbGames =>
                      a(href := s"${routes.User.games(u.username, "search")}?players.b=${r.user.username}"):
                        trans.site.nbGames.plural(nbGames, nbGames.localize)
                    }
                  ,
                  td:
                    views.relation
                      .actions(r.user.light, r.relation, followable = r.followable, blocked = false)
                )
            else tr(td(trans.site.none()))
      )

  private def page(title: String)(using Context) =
    Page(title)
      .cssTag("relation")
      .js(infiniteScrollEsmInit)
      .wrap: body =>
        main(cls := "box page-small")(body)

  private def pagTable(pager: Paginator[Related[UserWithPerfs]], call: Call)(using ctx: Context) =
    table(cls := "slist slist-pad")(
      if pager.nbResults > 0
      then
        tbody(cls := "infinite-scroll")(
          pager.currentPageResults.map: r =>
            tr(cls := "paginated")(
              td(userLink(r.user)),
              ctx.pref.showRatings.option(td(showBestPerf(r.user.perfs))),
              td(trans.site.nbGames.plural(r.user.count.game, r.user.count.game.localize)),
              td(actions(r.user.light, relation = r.relation, followable = r.followable, blocked = false))
            ),
          pagerNextTable(pager, np => addQueryParam(call.url, "page", np.toString))
        )
      else tbody(tr(td(colspan := 2)(trans.site.none())))
    )
