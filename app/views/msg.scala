package views.html

import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object msg {

  def home(json: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("msg"),
      moreJs = frag(
        jsTag("misc.expand-text"),
        moduleJsTag(
          "msg",
          Json.obj(
            "data" -> json
          )
        )
      ),
      title = s"Lishogi ${trans.inbox.txt()}"
    ) {
      main(cls := "box msg-app")
    }

}
