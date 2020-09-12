package views.html

import controllers.routes
import scala.util.chaining._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.oauth.AccessToken

object dgt {

  private val liveChessVersion = "2.2.5+"

  def index(implicit ctx: Context) =
    layout("index")(
      h1("Lichess <3 DGT (BETA)"),
      p(
        "This page allows you to connect your DGT board to Lichess, and to use it for playing games."
      ),
      br,
      br,
      st.section(
        h2("DGT Board Requirements"),
        br,
        p(
          s"To connect to the DGT Electronic Board you will need to install DGT LiveChess $liveChessVersion."
        ),
        p(
          "You can download the software here: ",
          a(href := "http://www.livechesscloud.com/software/")(s"LiveChess $liveChessVersion"),
          "."
        ),
        p(
          "If LiveChess is running on this computer, you can check your connection to it by ",
          a(href := "http://localhost:1982/doc/index.html")("opening this link"),
          "."
        )
      ),
      p(
        "If LiveChess is running on a different machine or different port,",
        "you will need to set the IP address and port here in the ",
        a(href := routes.DgtCtrl.config())("Configuration Section"),
        "."
      )
    )

  def play(token: AccessToken)(implicit ctx: Context) =
    layout("play", embedJsUnsafeLoadThen(s"""lichessDgt.playPage("${token.id.value}")"""))(
      h1("DGT - play"),
      div(id := "dgt-play-zone")("Do the thing here.")
    )

  def config(token: Option[lila.oauth.AccessToken])(implicit ctx: Context) =
    layout("config", embedJsUnsafeLoadThen("lichessDgt.configPage()"))(
      div(cls := "account")(
        h1("DGT - configure"),
        form(action := routes.DgtCtrl.generateToken(), method := "post")(
          st.section(
            h2("Lichess connectivity"),
            if (token.isDefined)
              p(cls := "text", dataIcon := "E")("You have an OAuth token suitable for DGT play.")
            else
              frag(
                p("No suitable OAuth token available yet."),
                form3.submit("Click to generate one")
              )
          )
        ),
        form(cls := "form3", id := "dgt-config")(
          st.section(
            h2("DGT board connectivity"),
            "dgt-livechess-url" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  s"LiveChess $liveChessVersion WebSocket URL"
                ),
                st.input(id := name, st.name := name, cls := "form-control", required := true),
                st.small(cls := "form-help")(
                  """Use "ws://localhost:1982/api/v1.0" unless LiveChess is running on a different machine or different port."""
                )
              )
            }
          ),
          st.section(
            h2("Text to speech"),
            div(cls := "form-group")(
              p("Configure voice narration of the played moves, so you can keep your eyes on the board.")
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")("Enable Speech Synthesis"),
              radios(
                "dgt-speech-synthesis",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              )
            ),
            "dgt-speech-voice" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")(
                  s"Speech synthesis voice"
                ),
                st.select(id := name, st.name := name, cls := "form-control")
              )
            },
            div(cls := "form-group")(
              st.label(cls := "form-label")("Announce All Moves"),
              radios(
                "dgt-speech-announce-all-moves",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              ),
              st.small(cls := "form-help")(
                """Select YES to annouce both your moves and your opponent's moves. Select NO to annouce only your opponent's moves."""
              )
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")("Announce Move Format"),
              radios(
                "dgt-speech-announce-move-format",
                List(("san", "SAN (Nf6)"), ("uci", "UCI (g8f6)"))
              ),
              st.small(cls := "form-help")(
                """San is the standard on Lichess like "Nf6". UCI is common on engines like "g8f6""""
              )
            ),
            "dgt-speech-keywords" pipe { name =>
              div(cls := "form-group")(
                st.label(`for` := name, cls := "form-label")("Keywords"),
                st.textarea(
                  id := name,
                  st.name := name,
                  cls := "form-control",
                  maxlength := 600,
                  rows := 10
                ),
                st.small(cls := "form-help")(
                  """Keywords are in JSON format. They are used to translate moves and results into your language. Default is English, but feel free to change it."""
                )
              )
            }
          ),
          st.section(
            h2("Debug"),
            div(cls := "form-group")(
              st.label(cls := "form-label")("Verbose logging"),
              radios(
                "dgt-verbose",
                List((false, trans.no.txt()), (true, trans.yes.txt()))
              ),
              st.small(cls := "form-help")(
                """To see console message press Command + Option + C (Mac) or Control + Shift + C (Windows, Linux, Chrome OS)"""
              )
            )
          ),
          form3.submit(trans.save())
        )
      )
    )

  private def radios(name: String, options: Iterable[(Any, String)]) =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"${name}_${v._1}"
        div(
          input(
            st.id := id,
            tpe := "radio",
            value := v._1.toString,
            st.name := name
          ),
          label(`for` := id)(v._2)
        )
      }.toList
    )

  private def layout(path: String, jsCall: Frag = emptyFrag)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("dgt"),
      moreJs = frag(jsModule("dgt"), jsCall),
      title = "Play with a DGT board",
      csp = defaultCsp.withAnyWs.some
    )(
      main(cls := "page-menu dgt")(
        st.nav(cls := "page-menu__menu subnav")(
          a(cls := path.active("index"), href := routes.DgtCtrl.index())(
            "DGT board"
          ),
          a(cls := path.active("play"), href := routes.DgtCtrl.play())(
            "Play"
          ),
          a(cls := path.active("config"), href := routes.DgtCtrl.config())(
            "Configure"
          )
        ),
        div(cls := s"page-menu__content box box-pad dgt__$path")(body)
      )
    )
}
