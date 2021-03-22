package views.html
package forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def searchForm(search: String = "")(implicit ctx: Context) =
    div(cls := "box__top__actions")(
      form(cls := "search", action := routes.ForumPost.search())(
        input(name := "text", value := search, placeholder := trans.search.search.txt())
      )
    )

  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
}
