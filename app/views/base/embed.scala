package views.base

import lila.app.templating.Environment.{ *, given }

import lila.pref.SoundSet

object embed:

  def apply(title: String, cssModule: String)(body: Modifier*)(using ctx: EmbedContext) =
    frag(
      layout.ui.doctype,
      layout.ui.htmlTag(using ctx.lang)(
        head(
          layout.ui.charset,
          layout.ui.viewport,
          layout.ui.metaCsp(basicCsp.withNonce(ctx.nonce).withInlineIconFont),
          st.headTitle(title),
          layout.ui.systemThemeEmbedScript,
          layout.ui.pieceSprite(ctx.pieceSet.name),
          cssTag("theme-light"), // includes both light & dark colors
          cssTag(cssModule)
        ),
        st.body(cls := s"${ctx.bg} highlight ${ctx.boardClass}")(
          layout.ui.dataSoundSet := SoundSet.silent.key,
          layout.ui.dataAssetUrl,
          layout.ui.dataAssetVersion := assetVersion.value,
          layout.ui.dataTheme        := ctx.bg,
          layout.ui.dataPieceSet     := ctx.pieceSet.name,
          layout.ui.dataDev,
          body
        )
      )
    )
