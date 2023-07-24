package views.html.base

import shogi.format.forsyth.Sfen
import controllers.routes
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._

object bits {

  def mselect(id: String, current: Frag, items: List[Frag]) =
    div(cls := "mselect")(
      input(
        tpe   := "checkbox",
        cls   := "mselect__toggle fullscreen-toggle",
        st.id := s"mselect-$id"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items)
    )

  lazy val stage = a(
    href  := "https://lishogi.org",
    style := """
background: #7f1010;
color: #fff;
position: fixed;
bottom: 0;
left: 0;
padding: .5em 1em;
border-top-right-radius: 3px;
z-index: 99;
"""
  )(
    "This is an empty Lishogi preview website, go to lishogi.org instead"
  )

  val connectLinks =
    div(cls := "connect-links")(
      a(href := "https://twitter.com/lishogi", rel := "nofollow")("Twitter"),
      a(href := "https://discord.gg/YFtpMGg3rR", rel := "nofollow")("Discord")
    )

  def sfenAnalysisLink(sfen: Sfen)(implicit lang: Lang) =
    a(href := routes.UserAnalysis.parseArg(sfen.value.replace(" ", "_")))(trans.analysis())
}
