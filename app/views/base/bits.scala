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
}
