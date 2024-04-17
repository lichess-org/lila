package views.html.base

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.*
import lila.pref.SoundSet

object embed:

  def apply(title: String, cssModule: String)(body: Modifier*)(using ctx: EmbedContext) =
    frag(
      layout.bits.doctype,
      layout.bits.htmlTag(using ctx.lang)(
        head(
          layout.bits.charset,
          layout.bits.viewport,
          layout.bits.metaCsp(basicCsp.withNonce(ctx.nonce).withInlineIconFont),
          st.headTitle(title),
          layout.bits.systemThemeEmbedScript,
          layout.bits.pieceSprite(ctx.pieceSet),
          cssTag("theme-light"), // includes both light & dark colors
          cssTag(cssModule)
        ),
        st.body(cls := s"${ctx.bg} highlight ${ctx.boardClass}")(
          layout.dataSoundSet := SoundSet.silent.key,
          layout.dataAssetUrl,
          layout.dataAssetVersion := assetVersion.value,
          layout.dataTheme        := ctx.bg,
          layout.dataPieceSet     := ctx.pieceSet.name,
          layout.dataDev,
          body
        )
      )
    )
