package views.html

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object dgt {

  private val liveChessVersion = "2.2.5+"

  def index(implicit ctx: Context) =
    layout("index")(
      h1("Lichess <3 DGT"),
      p(
        "This page allows you to connect your DGT board to Lichess, and to use it for playing games."
      ),
      st.section(
        h2("DGT Board Requirements"),
        br,
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

  def play(implicit ctx: Context) =
    layout("play", embedJsUnsafeLoadThen("lichessDgt.playPage()"))(
      h1("DGT - play")
    )

  def config(token: Option[lila.oauth.AccessToken])(implicit ctx: Context) =
    layout("config", embedJsUnsafeLoadThen("lichessDgt.configPage()"))(
      div(cls := "account")(
        h1("DGT - configure"),
        st.section(
          h2("Lichess connectivity"),
          if (token.isDefined)
            p(cls := "text", dataIcon := "E")("You have an OAuth token suitable for DGT play.")
          else
            form(action := routes.DgtCtrl.generateToken(), method := "post")(
              p("No suitable OAuth token available yet."),
              form3.submit("Click to generate one")
            )
        ),
        form(cls := "form3")(
          st.section(
            h2("DGT board connectivity"),
            div(cls := "form-group")(
              st.label(`for` := "dgt-livechess-url", cls := "form-label")(
                s"LiveChess $liveChessVersion WebSocket URL"
              ),
              st.input(id := "dgt-livechess-url", cls := "form-control", required := true),
              st.small(cls := "form-help")(
                """Use "ws://localhost:1982/api/v1.0" unless LiveChess is running on a different machine or different port."""
              )
            )
          ),
          st.section(
            h2("Text to speech"),
            div(cls := "form-group")(
              p("Configure voice narration of the played moves, so you can keep your eyes on the board.")
            ),
            div(cls := "form-group")(
              st.label(cls := "form-label")("Enable Speech Synthesis"),
              radios(
                "speech-synthesis",
                List((0, trans.no.txt()), (1, trans.yes.txt()))
              )
            )
          )
          // <div class="form-group">
          //     <div class="form-group">
          //         <label class="form-label" for="voiceSelect">Voice</label>
          //         <select id="voiceSelect" name="voiceSelect" class="form-control"></select>
          //     </div>
          // </div>
          // <div class="form-group">
          // <label class="form-label" for="form3-announceAllMoves">Announce All Moves</label>
          // <group class="radio">
          //     <div><input id="announceAllMoves_0" checked="checked" class="active" type="radio" value="false" name="announceAllMoves"><label for="announceAllMoves_0">No</label></div>
          //     <div><input id="announceAllMoves_1" type="radio" value="true" name="announceAllMoves"><label for="announceAllMoves_1">Yes</label></div>
          // </group>
          // <small class="form-help">Select YES to annouce both your moves and your opponent's moves. Select NO to annouce only your opponent's moves.</small>
          // </div>
          // <div class="form-group">
          // <label class="form-label" for="form3-announceMoveFormat">Announce Move Format</label>
          // <group class="radio">
          //     <div><input id="announceMoveFormat_0" checked="checked" class="active" type="radio" value="san" name="announceMoveFormat"><label for="announceMoveFormat_0">SAN</label></div>
          //     <div><input id="announceMoveFormat_1" type="radio" value="uci" name="announceMoveFormat"><label for="announceMoveFormat_1">UCI</label></div>
          // </group>
          // <small class="form-help">San is the standard on Lichess like "Nf6". UCI is common on engines like "g8f6"</small>
          // </div>
        )
      )
    )

  private def radios(name: String, options: Iterable[(Any, String)]) =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"dgt-${name}_${v._1}"
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
      title = "Play with a DGT board"
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
