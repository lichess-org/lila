package views.html.streamer

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

object index:

  import trans.streamer.*

  private val dataDedup = attr("data-dedup")

  def apply(
      live: List[lila.streamer.Streamer.WithUserAndStream],
      pager: Paginator[lila.streamer.Streamer.WithContext],
      requests: Boolean
  )(using ctx: PageContext) =

    val title = if requests then "Streamer approval requests" else lichessStreamers.txt()

    def widget(s: lila.streamer.Streamer.WithContext, stream: Option[lila.streamer.Stream]) =
      frag(
        if requests then a(href := s"${routes.Streamer.edit}?u=${s.user.username}", cls := "overlay")
        else bits.redirectLink(s.user.username, stream.isDefined.some)(cls := "overlay"),
        stream.isDefined option span(cls := "live-ribbon")(span(trans.streamer.live())),
        picture.thumbnail(s.streamer, s.user),
        div(cls := "overview")(
          bits.streamerTitle(s),
          s.streamer.headline.map(_.value).map { d =>
            p(
              cls := s"headline ${
                  if d.length < 60 then "small" else if d.length < 120 then "medium" else "large"
                }"
            )(d)
          },
          div(cls := "services")(
            s.streamer.twitch.map: twitch =>
              div(cls := "service twitch")(twitch.minUrl),
            s.streamer.youTube.map: youTube =>
              div(cls := "service youTube")(youTube.minUrl)
          ),
          div(cls := "ats"):
            stream
              .map: s =>
                p(cls := "at")(currentlyStreaming(strong(s.cleanStatus)))
              .getOrElse:
                frag(
                  p(cls := "at")(trans.lastSeenActive(momentFromNow(s.streamer.seenAt))),
                  s.streamer.liveAt.map: liveAt =>
                    p(cls := "at")(lastStream(momentFromNow(liveAt)))
                )
          ,
          div(cls := "streamer-footer")(
            !requests option bits.subscribeButtonFor(s),
            bits.streamerProfile(s)
          )
        )
      )

    views.html.base.layout(
      title = title,
      moreCss = cssTag("streamer.list"),
      moreJs = frag(infiniteScrollTag, jsModule("streamer"))
    ) {
      main(cls := "page-menu")(
        bits.menu(if requests then "requests" else "index", none)(cls := " page-menu__menu"),
        div(cls := "page-menu__content box streamer-list")(
          boxTop(h1(dataIcon := licon.Mic, cls := "text")(title)),
          !requests option div(cls := "list force-ltr live")(
            live.map: s =>
              st.article(cls := "streamer")(widget(s, s.stream))
          ),
          div(cls := "list force-ltr infinite-scroll")(
            (live.size % 2 == 1) option div(cls := "none"),
            pager.currentPageResults.map: s =>
              st.article(cls := "streamer paginated", dataDedup := s.streamer.id.value)(widget(s, none)),
            pagerNext(
              pager,
              np =>
                addQueryParams(
                  routes.Streamer.index().url,
                  Map(
                    "page"     -> np.toString,
                    "requests" -> (if requests then 1 else 0).toString
                  )
                )
            )
          )
        )
      )
    }
