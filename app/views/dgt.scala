package views.html

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object dgt {

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
        p("To connect to the DGT Electronic Board you will need to install DGT LiveChess 2.2.5 or later."),
        p(
          "You can download the software here: ",
          a(href := "http://www.livechesscloud.com/software/")("LiveChess 2.X"),
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

  def config(implicit ctx: Context) =
    layout("config", embedJsUnsafeLoadThen("lichessDgt.configPage()"))(
      h1("DGT - configure")
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
