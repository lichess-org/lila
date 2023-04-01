package views.html.simul

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object homeInner:

  def apply(
      pendings: List[lila.simul.Simul],
      createds: List[lila.simul.Simul],
      starteds: List[lila.simul.Simul],
      finisheds: List[lila.simul.Simul]
  )(using ctx: Context) =
    div(cls := "box")(
      h1(cls := "box__top")(trans.simultaneousExhibitions()),
      table(cls := "slist slist-pad")(
        pendings.nonEmpty option frag(
          thead(
            tr(
              th("Your pending simuls"),
              th(cls := "host")(trans.host()),
              th(cls := "players")(trans.players())
            )
          ),
          tbody(
            pendings.map { sim =>
              tr(cls := "scheduled")(
                simTd(sim),
                simHost(sim),
                td(cls := "players text", dataIcon := "")(sim.applicants.size)
              )
            }
          )
        ),
        thead(
          tr(
            th(trans.createdSimuls()),
            th(cls := "host")(trans.host()),
            th(cls := "players")(trans.players())
          )
        ),
        tbody(
          createds.map { sim =>
            tr(cls := "scheduled")(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := "")(sim.applicants.size)
            )
          },
          tr(cls := "create")(
            td(colspan := "4")(
              if (ctx.isAuth)
                a(href := routes.Simul.form, cls := "action button text")(trans.hostANewSimul())
              else
                a(href := routes.Auth.signup, cls := "action button text")("Sign up to host or join a simul")
            )
          )
        ),
        starteds.nonEmpty option (
          frag(
            thead(
              tr(
                th(trans.eventInProgress()),
                th(cls := "host")(trans.host()),
                th(cls := "players")(trans.players())
              )
            ),
            starteds.map { sim =>
              tr(
                simTd(sim),
                simHost(sim),
                td(cls := "players text", dataIcon := "")(sim.pairings.size)
              )
            }
          )
        ),
        thead(
          tr(
            th(trans.finished()),
            th(cls := "host")(trans.host()),
            th(cls := "players")(trans.players())
          )
        ),
        tbody(
          finisheds.map { sim =>
            tr(
              simTd(sim),
              simHost(sim),
              td(cls := "players text", dataIcon := "")(sim.pairings.size)
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

  private def simHost(sim: lila.simul.Simul)(using ctx: Context) =
    td(cls := "host")(
      userIdLink(sim.hostId.some, withOnline = false),
      ctx.pref.showRatings option frag(
        br,
        strong(sim.hostRating)
      )
    )
