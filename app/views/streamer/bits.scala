package views.html.streamer

import play.twirl.api.Html

import controllers.routes
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def liveStreams(l: lila.streamer.LiveStreams.WithTitles): Frag =
    l.live.streams.map { s =>
      a(cls := "stream highlight", href := routes.Streamer.show(s.streamer.id.value), title := s.status)(
        span(cls := "text", dataIcon := "")(l titleName s),
        " ",
        s.status
      )
    }

  object svg {

    val twitch = Html("""
<svg role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
  <path d="M2.089 0L.525 4.175v16.694h5.736V24h3.132l3.127-3.132h4.695l6.26-6.258V0H2.089zm2.086 2.085H21.39v11.479l-3.652 3.652H12l-3.127 3.127v-3.127H4.175V2.085z"/><path d="M9.915 12.522H12v-6.26H9.915v6.26zm5.735 0h2.086v-6.26H15.65v6.26z"/>
</svg>
""")

    val youTube = Html("""
<svg role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
  <path d="M23.495 6.205a3.007 3.007 0 0 0-2.088-2.088c-1.87-.501-9.396-.501-9.396-.501s-7.507-.01-9.396.501A3.007 3.007 0 0 0 .527 6.205a31.247 31.247 0 0 0-.522 5.805 31.247 31.247 0 0 0 .522 5.783 3.007 3.007 0 0 0 2.088 2.088c1.868.502 9.396.502 9.396.502s7.506 0 9.396-.502a3.007 3.007 0 0 0 2.088-2.088 31.247 31.247 0 0 0 .5-5.783 31.247 31.247 0 0 0-.5-5.805zM9.609 15.601V8.408l6.264 3.602z"/>
</svg>
""")

    val lichess = Html("""
<svg role="img" xmlns="http://www.w3.org/2000/svg" viewBox="5 5 40 40">
<path d="M 22,10 C 32.5,11 38.5,18 38,39 L 15,39 C 15,30 25,32.5 23,18"/>
<path d="M 24,18 C 24.38,20.91 18.45,25.37 16,27 C 13,29 13.18,31.34 11,31 C 9.958,30.06 12.41,27.96 11,28 C 10,28 11.19,29.23 10,30 C 9,30 5.997,31 6,26 C 6,24 12,14 12,14 C 12,14 13.89,12.1 14,10.5 C 13.27,9.506 13.5,8.5 13.5,7.5 C 14.5,6.5 16.5,10 16.5,10 L 18.5,10 C 18.5,10 19.28,8.008 21,7 C 22,7 22,10 22,10"/>
</svg>
""")
  }
}
