package views.html.relay

import views.html.base.layout.{ bits => layout }

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }

object embed {

  import EmbedConfig.implicits._

  def notFound(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp),
          st.headTitle(s"404 - ${trans.broadcast.broadcastNotFound()}"),
          cssTagWithTheme("analyse.embed", "dark")
        ),
        body(cls := "dark")(
          div(cls := "not-found")(
            h1(trans.broadcast.broadcastNotFound())
          )
        )
      )
    )
}
