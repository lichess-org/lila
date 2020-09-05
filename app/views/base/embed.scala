package views.html.base

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.app.ui.EmbedConfig

object embed {

  import EmbedConfig.implicits._

  def apply(title: String, cssModule: String)(body: Modifier*)(implicit config: EmbedConfig) =
    frag(
      layout.bits.doctype,
      layout.bits.htmlTag(config.lang)(
        head(
          layout.bits.charset,
          layout.bits.metaCsp(basicCsp),
          st.headTitle(title),
          layout.bits.pieceSprite(lila.pref.PieceSet.default),
          cssTagWithTheme(cssModule, config.bg)
        ),
        st.body(cls := s"base ${config.board}")(
          body
        )
      )
    )
}
