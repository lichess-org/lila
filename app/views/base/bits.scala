package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def mselect(id: String, current: Frag, items: List[Frag]) = div(cls := "mselect")(
    input(tpe := "checkbox", cls := "mselect__toggle fullscreen-toggle", st.id := s"mselect-$id", aria.label := "Other variants"),
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
      "This is an empty lichess preview website, go to lichess.org instead"
    )
}
