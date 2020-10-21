package views.html.base

import chess.format.FEN
import controllers.routes
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._

object bits {

  def mselect(id: String, current: Frag, items: List[Frag]) =
    div(cls := "mselect")(
      input(
        tpe := "checkbox",
        cls := "mselect__toggle fullscreen-toggle",
        st.id := s"mselect-$id",
        autocomplete := "off",
        aria.label := "Other variants"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items)
    )

  lazy val stage = a(
    href := "https://lichess.org",
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
    "This is an empty Lichess preview website, go to lichess.org instead"
  )

  val connectLinks =
    div(cls := "connect-links")(
      a(href := "https://twitter.com/lichess", rel := "nofollow")("Twitter"),
      a(href := "https://discord.gg/hy5jqSs", rel := "nofollow")("Discord"),
      a(href := "https://www.youtube.com/channel/UCr6RfQga70yMM9-nuzAYTsA", rel := "nofollow")("YouTube"),
      a(href := "https://www.twitch.tv/lichessdotorg", rel := "nofollow")("Twitch")
    )

  def fenAnalysisLink(fen: FEN)(implicit lang: Lang) =
    a(href := routes.UserAnalysis.parseArg(fen.value.replace(" ", "_")))(trans.analysis())
}
