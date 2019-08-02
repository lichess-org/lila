package views.html.streamer

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

object bits {

  def create(me: User)(implicit ctx: Context) = views.html.site.message(
    title = "Become a lidraughts streamer",
    icon = Some(""),
    back = false,
    moreCss = cssTag("streamer.form").some
  )(
      postForm(cls := "streamer-new", action := routes.Streamer.create)(
        h2("Do you have a Twitch or YouTube stream, ", me.username, "?"),
        br, br,
        bits.rules(),
        br, br,
        p(style := "text-align: center")(
          submitButton(cls := "button button-fat text", dataIcon := "")("Here we go!")
        )
      )
    )

  def pic(s: lidraughts.streamer.Streamer, u: User, size: Int = 300)(implicit ctx: Context) = s.picturePath match {
    case Some(path) => img(
      width := size,
      height := size,
      cls := "picture",
      src := dbImageUrl(path.value),
      alt := s"${u.titleUsername} lidraughts streamer"
    )
    case _ => img(
      width := size,
      height := size,
      cls := "default picture",
      src := staticUrl("images/streamer-nopic.svg"),
      alt := "Default streamer picture"
    )
  }

  def menu(active: String, s: Option[lidraughts.streamer.Streamer.WithUser])(implicit ctx: Context) =
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
      a(dataIcon := "", cls := "text", href := "/blog/XsAf0xIAACYAopgo/youtube--twitch-integration-on-lidraughts")("Streamer community")
    )

  def liveStreams(l: lidraughts.streamer.LiveStreams.WithTitles): Frag =
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

    val lidraughts = raw("""
<svg role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 210 210">
<g fill="none" stroke="#000" stroke-linejoin="round" stroke-linecap="round" stroke-width="6"><g fill="#fff">
<path d="M 10,140 c 0,60 190,60 190,0 l 0,-35 l -190,0 z"/>
<path d="M 10,105 c 0,60 190,60 190,0 l 0,-35 l -190,0 z"/>
<path d="M 10,70 c 0,60 190,60 190,0 c 0,-60 -190,-60 -190,0 z"/>
</g></g>
</svg>
""")
  }

  def rules = ul(cls := "streamer-rules")(
    li("Be listed as a lidraughts streamer."),
    li(title := "For example: Blitz battle on lidraughts.org")("We will list your stream on lidraughts when you stream with the keyword \"lidraughts.org\" in the stream title."),
    li("Notify your lidraughts followers when you start streaming."),
    li("Promote your stream in your games and tournaments.")
  )
}
