package views.html.base

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
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
          layout.bits.pieceSprite(ctx.pieceSet),
          cssTagWithDirAndTheme(cssModule, isRTL = lila.i18n.LangList.isRTL(ctx.lang), ctx.bg),
          ctx.bg == "system" option embedJsUnsafe(systemThemePolyfillJs, ctx.nonce)
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
