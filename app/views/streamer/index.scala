package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

object index {

  private val dataDedup = attr("data-dedup")

  def apply(
    live: List[lila.streamer.Streamer.WithUserAndStream],
    pager: Paginator[lila.streamer.Streamer.WithUser],
    requests: Boolean
  )(implicit ctx: Context) = {

    val title = if (requests) "Streamer approval requests" else "Lichess streamers"

    def widget(s: lila.streamer.Streamer.WithUser, stream: Option[lila.streamer.Stream]) = frag(
      a(
        cls := "overlay",
        href := {
          if (requests) s"${routes.Streamer.edit}?u=${s.user.username}"
          else routes.Streamer.show(s.user.username).url
        }
      ),
      stream.isDefined option span(cls := "ribbon")(span("LIVE!")),
      bits.pic(s.streamer, s.user),
      div(cls := "overview")(
        h1(titleTag(s.user.title), stringValueFrag(s.streamer.name)),
        s.streamer.headline.map(_.value).map { d =>
          p(cls := s"headline ${if (d.size < 60) "small" else if (d.size < 120) "medium" else "large"}")(d)
        },
        div(cls := "services")(
          s.streamer.twitch.map { twitch =>
            div(cls := "service twitch")(
              bits.svg.twitch,
              " ",
              twitch.minUrl
            )
          },
          s.streamer.youTube.map { youTube =>
            div(cls := "service youTube")(
              bits.svg.youTube,
              " ",
              youTube.minUrl
            )
          }
        ),
        div(cls := "ats")(
          stream.map { s =>
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
        )
      )
    )

    views.html.base.layout(
      title = title,
      responsive = true,
      moreCss = responsiveCssTag("streamer.list"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "page-menu")(
          bits.menu("index", none)(ctx)(cls := " page-menu__menu"),
          div(cls := "page-menu__content box streamer-list")(
            h1(dataIcon := "", cls := "text")(title),
            !requests option div(cls := "list live")(
              live.map { s =>
                st.article(cls := "streamer")(widget(s.withoutStream, s.stream))
              }
            ),
            div(cls := "list infinitescroll")(
              pager.currentPageResults.map { s =>
                st.article(cls := "streamer paginated", dataDedup := s.streamer.id.value)(widget(s, none))
              },
              pagerNext(pager, np => addQueryParameter(routes.Streamer.index().url, "page", np))
            )
          )
        )
      }
  }
}
