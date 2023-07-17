package views.html.streamer

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText
import lila.streamer.Stream.YouTube
import lila.user.{ User, UserPerfs }

object show:

  import trans.streamer.*

  def apply(
      s: lila.streamer.Streamer.WithUserAndStream,
      perfs: UserPerfs,
      activities: Vector[lila.activity.ActivityView]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${s.titleName} streams chess",
      moreCss = cssTag("streamer.show"),
      moreJs = jsModule("streamer"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${s.titleName} streams chess",
          description =
            shorten(~(s.streamer.headline.map(_.value) orElse s.streamer.description.map(_.value)), 152),
          url = s"$netBaseUrl${routes.Streamer.show(s.user.username)}",
          `type` = "video",
          image = s.streamer.hasPicture option picture.thumbnail.url(s.streamer)
        )
        .some,
      csp = defaultCsp.finalizeWithTwitch.some
    ):
      main(cls := "page-menu streamer-show")(
        st.aside(cls := "page-menu__menu")(
          s.streamer.approval.chatEnabled option div(cls := "streamer-chat")(
            s.stream match
              case Some(YouTube.Stream(_, _, videoId, _, _)) =>
                iframe(
                  frame.credentialless,
                  st.frameborder  := "0",
                  frame.scrolling := "no",
                  src := s"https://www.youtube.com/live_chat?v=$videoId&embed_domain=${netConfig.domain}"
                )
              case _ =>
                s.streamer.twitch.map { twitch =>
                  iframe(
                    frame.credentialless,
                    st.frameborder  := "0",
                    frame.scrolling := "yes",
                    src := s"https://twitch.tv/embed/${twitch.userId}/chat?${(ctx.pref.currentBg != "light") so "darkpopout&"}parent=${netConfig.domain}"
                  )
                }
          ),
          bits.menu("show", s.some)
        ),
        div(cls := "page-menu__content")(
          s.stream match
            case Some(YouTube.Stream(_, _, videoId, _, _)) =>
              div(cls := "box embed youTube")(
                iframe(
                  src            := s"https://www.youtube.com/embed/$videoId?autoplay=1",
                  st.frameborder := "0",
                  frame.allowfullscreen,
                  frame.credentialless
                )
              )
            case _ =>
              s.streamer.twitch.map { twitch =>
                div(cls := "box embed twitch")(
                  iframe(
                    src := s"https://player.twitch.tv/?channel=${twitch.userId}&parent=${netConfig.domain}",
                    frame.allowfullscreen,
                    frame.credentialless
                  )
                )
              } getOrElse div(cls := "box embed")(div(cls := "nostream")(offline()))
          ,
          div(cls := "box streamer")(
            views.html.streamer.header(s),
            div(cls := "description")(richText(s.streamer.description.fold("")(_.value))),
            ctx.pref.showRatings option a(cls := "ratings", href := routes.User.show(s.user.username)):
              perfs.best6Perfs.map { showPerfRating(perfs, _) }
            ,
            views.html.activity(User.WithPerfs(s.user, perfs), activities)
          )
        )
      )
