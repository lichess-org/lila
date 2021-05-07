package views.html.video

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(
      title: String,
      control: lila.video.UserControl,
      openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("video"),
      moreJs = infiniteScrollTag,
      wrapClass = "full-screen-force",
      openGraph = openGraph
    ) {
      main(cls := "video page-menu")(
        st.aside(cls := "page-menu__menu")(
          div(cls := "subnav")(
            control.tags.map { t =>
              val checked = control.filter.tags contains t.tag
              a(
                cls := List(
                  "checked" -> checked,
                  "full"    -> (checked || t.nb > 0),
                  "empty"   -> !(checked || t.nb > 0)
                ),
                href := (checked || t.nb > 0) option s"${routes.Video.index}?${control.toggleTag(t.tag).queryString}"
              )(
                span(t.tag.capitalize),
                (!checked && t.nb > 0) option em(t.nb)
              )
            }
          ),
          div(cls := "under-tags")(
            if (control.filter.tags.nonEmpty)
              a(cls := "button button-empty", href := routes.Video.index)("Clear search")
            else
              a(dataIcon := "o", href := routes.Video.tags)("View more tags")
          )
        ),
        div(cls := "page-menu__content box")(body)
      )
    }
}
