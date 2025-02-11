package lila.simul
package ui

import scalalib.paginator.Paginator

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SimulHome(helpers: Helpers, ui: SimulUi):
  import helpers.{ *, given }

  def apply(
      pendings: List[Simul],
      opens: List[Simul],
      starteds: List[Simul],
      finisheds: List[Simul]
  )(using ctx: Context) =
    Page(trans.site.simultaneousExhibitions.txt())
      .css("simul.list")
      .js(Esm("simul.home"))
      .graph(
        title = trans.site.simultaneousExhibitions.txt(),
        url = s"$netBaseUrl${routes.Simul.home}",
        description = trans.site.aboutSimul.txt()
      )
      .hrefLangs(lila.ui.LangPath(routes.Simul.home)):
        main(cls := "page-menu simul-list")(
          st.aside(cls := "page-menu__menu simul-list__help")(
            p(trans.site.aboutSimul()),
            img(src := assetUrl("images/fischer-simul.jpg"), alt := "Simul IRL with Bobby Fischer")(
              em("[1964] ", trans.site.aboutSimulImage()),
              p(trans.site.aboutSimulRealLife()),
              p(trans.site.aboutSimulRules()),
              p(trans.site.aboutSimulSettings())
            )
          ),
          div(cls := "page-menu__content simul-list__content")(
            homeInner(pendings, opens, starteds, finisheds)
          )
        )

  def homeInner(
      pendings: List[Simul],
      createds: List[Simul],
      starteds: List[Simul],
      finisheds: List[Simul]
  )(using ctx: Context) =
    div(cls := "box")(
      h1(cls := "box__top")(trans.site.simultaneousExhibitions()),
      table(cls := "slist slist-pad")(
        pendings.nonEmpty.option(
          frag(
            thead(
              tr(
                th(trans.site.yourPendingSimuls()),
                th(cls := "host")(trans.site.host()),
                th(cls := "players")(trans.site.players())
              )
            ),
            tbody(
              pendings.map: sim =>
                tr(cls := "scheduled")(
                  simTd(sim),
                  simHost(sim),
                  td(cls := "players text", dataIcon := Icon.User)(sim.applicants.size)
                )
            )
          )
        ),
        thead(
          tr(
            th(trans.site.createdSimuls()),
            th(cls := "host")(trans.site.host()),
            th(cls := "players")(trans.site.players())
          )
        ),
        tbody(
          createds.map: sim =>
            tr(cls := "scheduled")(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := Icon.User)(sim.applicants.size)
            ),
          tr(cls := "create")(
            td(colspan := "4")(
              if ctx.isAuth then
                a(href := routes.Simul.form, cls := "action button text")(trans.site.hostANewSimul())
              else
                a(href := routes.Auth.signup, cls := "action button text")(
                  trans.site.signUpToHostOrJoinASimul()
                )
            )
          )
        ),
        starteds.nonEmpty.option(
          frag(
            thead(
              tr(
                th(trans.site.eventInProgress()),
                th(cls := "host")(trans.site.host()),
                th(cls := "players")(trans.site.players())
              )
            ),
            starteds.map: sim =>
              tr(
                simTd(sim),
                simHost(sim),
                td(cls := "players text", dataIcon := Icon.User)(sim.pairings.size)
              )
          )
        ),
        thead(
          tr(
            th(trans.site.finished()),
            th(cls := "host")(trans.site.host()),
            th(cls := "players")(trans.site.players())
          )
        ),
        tbody(
          finisheds.map: sim =>
            tr(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := Icon.User)(sim.pairings.size)
            )
        )
      )
    )

  private def simTd(sim: Simul) =
    td(cls := "header")(
      a(href := routes.Simul.show(sim.id))(
        span(cls := "name")(sim.fullName),
        ui.setup(sim)
      )
    )

  private def simHost(sim: Simul)(using ctx: Context) =
    td(cls := "host")(
      userIdLink(sim.hostId.some, withOnline = false),
      ctx.pref.showRatings.option(
        frag(
          br,
          strong(sim.hostRating)
        )
      )
    )

  def hosted(user: User, pager: Paginator[Simul])(using Context) =
    Page(s"${user.username} hosted simuls")
      .css("simul.user.list")
      .js(infiniteScrollEsmInit):
        main(cls := "page-small box simul-list")(
          if pager.nbResults == 0 then
            div(cls := "box__top")(h1(userLink(user), " hasn't hosted any simuls yet!"))
          else
            table(cls := "slist slist-pad")(
              thead(
                tr(
                  th(cls := "count")(pager.nbResults),
                  th(colspan := 2)(h1(userLink(user, withOnline = true), " simuls")),
                  th(s"${trans.site.wins.txt()}/${trans.site.draws.txt()}/${trans.site.losses.txt()}")
                )
              ),
              tbody(cls := "infinite-scroll")(
                pager.currentPageResults.map: s =>
                  val hostColor = s.color | "random"
                  tr(cls := "paginated")(
                    td(cls := "icon")(iconTag(s.mainPerfType.icon)),
                    td(cls := "name")(a(href := routes.Simul.show(s.id))(s.fullName)),
                    td(
                      span(cls := s"color-icon is $hostColor text", title := hostColor)(s.clock.config.show),
                      br,
                      momentFromNow(s.createdAt)
                    ),
                    td(s"${s.wins} / ${s.draws} / ${s.losses}")
                  )
                ,
                pagerNextTable(pager, np => routes.Simul.byUser(user.username, np).url)
              )
            )
        )
