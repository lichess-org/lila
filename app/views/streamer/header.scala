package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object header {

  def apply(s: lila.streamer.Streamer.WithUserAndStream, following: Option[Boolean])(implicit ctx: Context) =
    div(cls := "top")(
      bits.pic(s.streamer, s.user, 300),
      div(cls := "overview")(
        h1(
          titleTag(s.user.title),
          s.streamer.name
        ),
        s.streamer.headline.map(_.value).map { d =>
          p(cls := s"headline ${if (d.size < 60) "small" else if (d.size < 120) "medium" else "large"}")(d)
        },
        div(cls := "services")(
          s.streamer.twitch.map { twitch =>
            a(
              cls := List(
                "service twitch" -> true,
                "live" -> s.stream.exists(_.twitch)
              ),
              href := twitch.fullUrl
            )(bits.svg.twitch, " ", twitch.minUrl)
          },
          s.streamer.youTube.map { youTube =>
            a(
              cls := List(
                "service youTube" -> true,
                "live" -> s.stream.exists(_.twitch)
              ),
              href := youTube.fullUrl
            )(bits.svg.youTube, " ", youTube.minUrl)
          },
          a(cls := "service lichess", href := routes.User.show(s.user.username))(
            bits.svg.lichess,
            " ",
            s"lichess.org/@/${s.user.username}"
          )
        ),
        div(cls := "ats")(
          s.stream.map { s =>
            p(cls := "at")(
              "Currently streaming: ",
              strong(s.status)
            )
          } getOrElse frag(
            p(cls := "at")(trans.lastSeenActive.frag(momentFromNow(s.streamer.seenAt))),
            s.streamer.liveAt.map { liveAt =>
              p(cls := "at")("Last stream ", momentFromNow(liveAt))
            }
          )
        ),
        following.map { f =>
          (ctx.isAuth && !ctx.is(s.user)) option
            button(attr("data-user") := s.user.id, cls := List(
              "follow icon button" -> true,
              "active" -> f
            ), tpe := "submit")(
              span(cls := "text", dataIcon := "h")(trans.follow.frag())
            )
        }
      )
    )
}
