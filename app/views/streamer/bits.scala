package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object bits {

  def create(me: User)(implicit ctx: Context) = views.html.site.message(
    title = "Become a lichess streamer",
    icon = Some(""),
    back = false,
    moreCss = cssTag("streamer.form").some
  )(
      form(cls := "streamer-new", action := routes.Streamer.create, method := "POST")(
        h2("Do you have a Twitch or YouTube stream, ", me.username, "?"),
        br, br,
        bits.rules(),
        br, br,
        p(style := "text-align: center")(
          button(tpe := "submit", cls := "button button-fat text", dataIcon := "")("Here we go!")
        )
      )
    )

  def pic(s: lila.streamer.Streamer, u: User, size: Int = 300)(implicit ctx: Context) = s.picturePath match {
    case Some(path) => img(
      width := size,
      height := size,
      cls := "picture",
      src := dbImageUrl(path.value),
      alt := s"${u.titleUsername} lichess streamer"
    )
    case _ => img(
      width := size,
      height := size,
      cls := "default picture",
      src := staticUrl("images/streamer-nopic.svg"),
      alt := "Default streamer picture"
    )
  }

  def menu(active: String, s: Option[lila.streamer.Streamer.WithUser])(implicit ctx: Context) =
    st.nav(cls := "subnav")(
      a(cls := active.active("index"), href := routes.Streamer.index())("All streamers"),
      s.map { st =>
        frag(
          a(cls := active.active("show"), href := routes.Streamer.show(st.streamer.id.value))(st.streamer.name),
          (ctx.is(st.user) || isGranted(_.Streamers)) option
            a(cls := active.active("edit"), href := s"${routes.Streamer.edit}?u=${st.streamer.id.value}")("Edit streamer page")
        )
      } getOrElse a(href := routes.Streamer.edit)("Your streamer page"),
      isGranted(_.Streamers) option a(cls := active.active("requests"), href := s"${routes.Streamer.index()}?requests=1")("Approval requests"),
      a(dataIcon := "", cls := "text", href := "/blog/Wk5z0R8AACMf6ZwN/join-the-lichess-streamer-community")("Streamer community"),
      a(href := "/about")("Download streamer kit")
    )

  def liveStreams(l: lila.streamer.LiveStreams.WithTitles): Frag =
    l.live.streams.map { s =>
      a(cls := "stream highlight", href := routes.Streamer.show(s.streamer.id.value), title := s.status)(
        strong(cls := "text", dataIcon := "")(l titleName s),
        " ",
        s.status
      )
    }

  def contextual(userId: User.ID): Frag =
    a(cls := "context-streamer text", dataIcon := "", href := routes.Streamer.show(userId))(
      usernameOrId(userId), " is streaming"
    )

  object svg {

    val twitch = raw("""
<svg role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
  <path d="M2.089 0L.525 4.175v16.694h5.736V24h3.132l3.127-3.132h4.695l6.26-6.258V0H2.089zm2.086 2.085H21.39v11.479l-3.652 3.652H12l-3.127 3.127v-3.127H4.175V2.085z"/><path d="M9.915 12.522H12v-6.26H9.915v6.26zm5.735 0h2.086v-6.26H15.65v6.26z"/>
</svg>
""")

    val youTube = raw("""
<svg role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
  <path d="M23.495 6.205a3.007 3.007 0 0 0-2.088-2.088c-1.87-.501-9.396-.501-9.396-.501s-7.507-.01-9.396.501A3.007 3.007 0 0 0 .527 6.205a31.247 31.247 0 0 0-.522 5.805 31.247 31.247 0 0 0 .522 5.783 3.007 3.007 0 0 0 2.088 2.088c1.868.502 9.396.502 9.396.502s7.506 0 9.396-.502a3.007 3.007 0 0 0 2.088-2.088 31.247 31.247 0 0 0 .5-5.783 31.247 31.247 0 0 0-.5-5.805zM9.609 15.601V8.408l6.264 3.602z"/>
</svg>
""")

    val lichess = raw("""
<svg role="img" xmlns="http://www.w3.org/2000/svg" viewBox="5 5 40 40">
<path d="M 22,10 C 32.5,11 38.5,18 38,39 L 15,39 C 15,30 25,32.5 23,18"/>
<path d="M 24,18 C 24.38,20.91 18.45,25.37 16,27 C 13,29 13.18,31.34 11,31 C 9.958,30.06 12.41,27.96 11,28 C 10,28 11.19,29.23 10,30 C 9,30 5.997,31 6,26 C 6,24 12,14 12,14 C 12,14 13.89,12.1 14,10.5 C 13.27,9.506 13.5,8.5 13.5,7.5 C 14.5,6.5 16.5,10 16.5,10 L 18.5,10 C 18.5,10 19.28,8.008 21,7 C 22,7 22,10 22,10"/>
</svg>
""")
  }

  def rules = ul(cls := "streamer-rules")(
    li("Be listed as a lichess streamer."),
    li(title := "For example: Blitz battle on lichess.org")("Get bumped up the top of the list when you stream with the keyword \"lichess.org\" in the stream title."),
    li("Notify your lichess followers when you start streaming."),
    li("Promote your stream in your games and tournaments.")
  )
}
