package views.base

import lila.app.templating.Environment.{ *, given }

def embed(title: String, cssModule: String, modules: EsmList = Nil)(body: Modifier*)(using
    ctx: EmbedContext
) =
  frag(
    layout.ui.doctype,
    layout.ui.htmlTag(using ctx.lang)(
      cls := ctx.bg,
      head(
        layout.ui.charset,
        layout.ui.viewport,
        layout.ui.metaCsp(basicCsp.withNonce(ctx.nonce).withInlineIconFont),
        st.headTitle(title),
        (ctx.bg == "system").option(layout.ui.systemThemeScript(ctx.nonce.some)),
        layout.ui.pieceSprite(ctx.pieceSet.name),
        cssTag("theme-light"), // includes both light & dark colors
        cssTag(cssModule),
        layout.ui.modulesPreload(modules, isInquiry = false)
      ),
      st.body(cls := s"highlight ${ctx.boardClass}")(
        layout.ui.dataSoundSet := lila.pref.SoundSet.silent.key,
        layout.ui.dataAssetUrl,
        layout.ui.dataAssetVersion := assetVersion.value,
        layout.ui.dataTheme        := ctx.bg,
        layout.ui.dataPieceSet     := ctx.pieceSet.name,
        layout.ui.dataDev,
        body
      )
    )
  )
