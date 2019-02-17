package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object widget {
  def apply(c: lila.coach.Coach.WithUser)(implicit ctx: Context) = {
    val profile = c.user.profileOrDefault
    frag(
      a(cls := "overlay", href := routes.Coach.show(c.user.username)),
      pic(c, 250),
      div(cls := "overview")(
        h1(
          c.user.title.map { t => s"t " },
          c.user.realNameOrUsername
        ),
        c.coach.profile.headline.map { h =>
          p(cls := s"headline ${if (h.size < 60) "small" else if (h.size < 120) "medium" else "large"}")(h)
        },
        table(
          tbody(
            tr(
              th("Location"),
              td(
                profile.nonEmptyLocation.map { l =>
                  span(cls := "location")(l)
                },
                profile.countryInfo.map { c =>
                  frag(
                    span(cls := "country")(
                      img(cls := "flag", src := staticUrl(s"images/flags/${c.code}.png")),
                      " ", c.name
                    )
                  )
                }
              )
            ),
            c.coach.profile.languages.map { l =>
              tr(cls := "languages")(
                th("Languages"),
                td(l)
              )
            },
            tr(cls := "rating")(
              th("Rating"),
              td(
                profile.fideRating.map { r =>
                  frag("FIDE: ", r)
                },
                a(href := routes.User.show(c.user.username))(
                  c.user.best8Perfs.take(6).filter(c.user.hasEstablishedRating).map {
                    showPerfRating(c.user, _)
                  }
                )
              )
            ),
            c.coach.profile.hourlyRate.map { r =>
              tr(cls := "rate")(
                th("Hourly rate"),
                td(r)
              )
            },
            tr(cls := "available")(
              th("Availability"),
              td(
                if (c.coach.available.value) span(cls := "text", dataIcon := "E")("Accepting students")
                else span(cls := "text", dataIcon := "L")("Not accepting students at the moment")
              )
            ),
            c.user.seenAt.map { seen =>
              tr(cls := "seen")(
                th,
                td(trans.lastSeenActive(momentFromNow(seen)))
              )
            }
          )
        )
      )
    )
  }
}
