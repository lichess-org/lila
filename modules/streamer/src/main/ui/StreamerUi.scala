package lila.streamer
package ui
import scalalib.paginator.Paginator

import lila.core.config.NetDomain
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StreamerUi(helpers: Helpers, bits: StreamerBits)(using netDomain: NetDomain):
  import helpers.{ *, given }
  import trans.streamer as trs

  private val dataDedup = attr("data-dedup")

  def csp: Update[ContentSecurityPolicy] = csp =>
    csp.copy(
      defaultSrc = Nil,
      connectSrc = "https://www.twitch.tv" :: "https://www-cdn.jtvnw.net" :: csp.connectSrc,
      styleSrc = Nil,
      frameSrc = Nil,
      workerSrc = Nil,
      scriptSrc = Nil
    )

  def index(
      live: List[Streamer.WithUserAndStream],
      pager: Paginator[Streamer.WithContext],
      requests: Boolean
  )(using ctx: Context) =

    def widget(s: Streamer.WithContext, stream: Option[Stream]) =
      frag(
        if requests then a(href := s"${routes.Streamer.edit}?u=${s.user.username}", cls := "overlay")
        else bits.redirectLink(s.user.username, stream.isDefined.some)(cls := "overlay"),
        stream.isDefined.option(span(cls := "live-ribbon")(span(trans.streamer.live()))),
        bits.thumbnail(s.streamer, s.user),
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
                p(cls := "at")(trs.currentlyStreaming(strong(s.cleanStatus)))
              .getOrElse:
                frag(
                  p(cls := "at")(trans.site.lastSeenActive(momentFromNow(s.streamer.seenAt))),
                  s.streamer.liveAt.map: liveAt =>
                    p(cls := "at")(trs.lastStream(momentFromNow(liveAt)))
                )
          ,
          div(cls := "streamer-footer")(
            (!requests).option(bits.subscribeButtonFor(s)),
            bits.streamerProfile(s)
          )
        )
      )

    val title = if requests then "Streamer approval requests" else trans.streamer.lichessStreamers.txt()
    Page(title)
      .css("bits.streamer.list")
      .js(infiniteScrollEsmInit)
      .js(esmInitBit("streamerSubscribe")):
        main(cls := "page-menu")(
          bits.menu(if requests then "requests" else "index", none)(cls := " page-menu__menu"),
          div(cls := "page-menu__content box streamer-list")(
            boxTop(h1(dataIcon := Icon.Mic, cls := "text")(title)),
            (!requests).option(
              div(cls := "list force-ltr live")(
                live.map: s =>
                  st.article(cls := "streamer")(widget(s, s.stream))
              )
            ),
            div(cls := "list force-ltr infinite-scroll")(
              (live.size % 2 == 1).option(div(cls := "none")),
              pager.currentPageResults.map: s =>
                st.article(cls := "streamer paginated", dataDedup := s.streamer.id.value)(widget(s, none)),
              pagerNext(
                pager,
                np =>
                  addQueryParams(
                    routes.Streamer.index().url,
                    Map(
                      "page" -> np.toString,
                      "requests" -> (if requests then 1 else 0).toString
                    )
                  )
              )
            )
          )
        )

  def show(s: Streamer.WithUserAndStream, perfRatings: Frag, activities: Frag)(using ctx: Context) =
    Page(s"${s.titleName} streams chess")
      .csp(csp)
      .css("bits.streamer.show")
      .js(esmInitBit("streamerSubscribe"))
      .graph(
        OpenGraph(
          title = s"${s.titleName} streams chess",
          description =
            shorten(~(s.streamer.headline.map(_.value).orElse(s.streamer.description.map(_.value))), 152),
          url = s"$netBaseUrl${routes.Streamer.show(s.user.username)}",
          `type` = "video",
          image = s.streamer.hasPicture.option(bits.thumbnail.url(s.streamer))
        )
      ):
        main(cls := "page-menu streamer-show")(
          st.aside(cls := "page-menu__menu")(
            s.streamer.approval.chatEnabled.option(
              div(cls := "streamer-chat")(
                s.stream match
                  case Some(Stream.YouTube.Stream(_, _, videoId, _, _)) =>
                    iframe(
                      frame.credentialless,
                      st.frameborder := "0",
                      frame.scrolling := "no",
                      src := s"https://www.youtube.com/live_chat?v=$videoId&embed_domain=$netDomain"
                    )
                  case _ =>
                    s.streamer.twitch.map: twitch =>
                      val darkChat = (ctx.pref.currentBg != "light").so("darkpopout&")
                      iframe(
                        frame.credentialless,
                        st.frameborder := "0",
                        frame.scrolling := "yes",
                        src := s"https://twitch.tv/embed/${twitch.userId}/chat?${darkChat}parent=$netDomain"
                      )
              )
            ),
            bits.menu("show", s.some)
          ),
          div(cls := "page-menu__content")(
            s.stream match
              case Some(Stream.YouTube.Stream(_, _, videoId, _, _)) =>
                div(cls := "box embed youTube")(
                  iframe(
                    src := s"https://www.youtube.com/embed/$videoId?autoplay=1",
                    st.frameborder := "0",
                    frame.allowfullscreen,
                    frame.credentialless
                  )
                )
              case _ =>
                s.streamer.twitch
                  .map: twitch =>
                    div(cls := "box embed twitch")(
                      iframe(
                        src := s"https://player.twitch.tv/?channel=${twitch.userId}&parent=$netDomain",
                        frame.allowfullscreen,
                        frame.credentialless
                      )
                    )
                  .getOrElse(div(cls := "box embed")(div(cls := "nostream")(trans.streamer.offline())))
            ,
            standardFlash,
            div(cls := "box streamer")(
              bits.header(s),
              div(cls := "description")(richText(s.streamer.description.fold("")(_.value))),
              ctx.pref.showRatings.option(a(cls := "ratings", href := routes.User.show(s.user.username)):
                perfRatings),
              activities
            )
          )
        )
