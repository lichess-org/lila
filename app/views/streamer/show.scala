package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.streamer.Stream.YouTube

object show {

  import trans.streamer._

  def apply(
      s: lila.streamer.Streamer.WithUserAndStream,
      activities: Vector[lila.activity.ActivityView],
      following: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = xStreamsShogi.text(s.titleName),
      moreCss = cssTag("streamer.show"),
      moreJs = frag(
        jsTag("ads.js"),
        embedJsUnsafe(
          """
$(function() {
$('button.follow').click(function() {
var klass = 'active';
$(this).toggleClass(klass);
$.ajax({
url: '/rel/' + ($(this).hasClass('active') ? 'follow/' : 'unfollow/') + $(this).data('user'),
method:'post'
});
});
});"""
        )
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          title = xStreamsShogi.text(s.titleName),
          description =
            shorten(~(s.streamer.headline.map(_.value) orElse s.streamer.description.map(_.value)), 152),
          url = s"$netBaseUrl${routes.Streamer.show(s.user.username)}",
          `type` = "video",
          image = s.streamer.picturePath.map(p => dbImageUrl(p.value))
        )
        .some,
      csp = defaultCsp.withTwitch.some
    )(
      main(cls := "page-menu streamer-show")(
        st.aside(cls := "page-menu__menu")(
          s.streamer.approval.chatEnabled option div(cls := "streamer-chat")(
            s.stream match {
              case Some(YouTube.Stream(_, _, videoId, _)) =>
                iframe(
                  st.frameborder := "0",
                  frame.scrolling := "no",
                  src := s"https://www.youtube.com/live_chat?v=$videoId&embed_domain=localhost"
                )
              case _ =>
                s.streamer.twitch.map { twitch =>
                  iframe(
                    st.frameborder := "0",
                    frame.scrolling := "yes",
                    src := s"https://twitch.tv/embed/${twitch.userId}/chat?${(ctx.currentBg != "light") ?? "darkpopout&"}parent=localhost"
                  )
                }
            }
          ),
          bits.menu("show", s.withoutStream.some),
          a(cls := "ads-vulnerable blocker none button button-metal", href := "https://getublockorigin.com")(
            i(dataIcon := ""),
            strong(installBlocker()),
            beSafe()
          )
        ),
        div(cls := "page-menu__content")(
          s.stream match {
            case Some(YouTube.Stream(_, _, videoId, _)) =>
              div(cls := "box embed youTube")(
                iframe(
                  src := s"https://www.youtube.com/embed/$videoId?autoplay=1",
                  st.frameborder := "0",
                  frame.allowfullscreen
                )
              )
            case _ =>
              s.streamer.twitch.map { twitch =>
                div(cls := "box embed twitch")(
                  iframe(
                    src := s"https://player.twitch.tv/?channel=${twitch.userId}&parent=localhost",
                    frame.allowfullscreen
                  )
                )
              } getOrElse div(cls := "box embed")(div(cls := "nostream")(offline()))
          },
          div(cls := "box streamer")(
            views.html.streamer.header(s, following.some),
            div(cls := "description")(richText(s.streamer.description.fold("")(_.value))),
            a(cls := "ratings", href := routes.User.show(s.user.username))(
              s.user.best6Perfs.map { showPerfRating(s.user, _) }
            ),
            views.html.activity(s.user, activities)
          )
        )
      )
    )
}
