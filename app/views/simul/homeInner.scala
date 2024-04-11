package views.html.simul

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object homeInner:

  def apply(
      pendings: List[lila.simul.Simul],
      createds: List[lila.simul.Simul],
      starteds: List[lila.simul.Simul],
      finisheds: List[lila.simul.Simul]
  )(using ctx: PageContext) =
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
              pendings.map { sim =>
                tr(cls := "scheduled")(
                  simTd(sim),
                  simHost(sim),
                  td(cls := "players text", dataIcon := Icon.User)(sim.applicants.size)
                )
              }
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
          createds.map { sim =>
            tr(cls := "scheduled")(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := Icon.User)(sim.applicants.size)
            )
          },
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
            starteds.map { sim =>
              tr(
                simTd(sim),
                simHost(sim),
                td(cls := "players text", dataIcon := Icon.User)(sim.pairings.size)
              )
            }
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
          finisheds.map { sim =>
            tr(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := Icon.User)(sim.pairings.size)
            )
          }
        )
      )
    )

  private def simTd(sim: lila.simul.Simul) =
    td(cls := "header")(
      a(href := routes.Simul.show(sim.id))(
        span(cls := "name")(sim.fullName),
        bits.setup(sim)
      )
    )

  private def simHost(sim: lila.simul.Simul)(using ctx: PageContext) =
    td(cls := "host")(
      userIdLink(sim.hostId.some, withOnline = false),
      ctx.pref.showRatings.option(
        frag(
          br,
          strong(sim.hostRating)
        )
      )
    )
