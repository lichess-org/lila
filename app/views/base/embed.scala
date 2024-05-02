package views.base

import lila.app.UiEnv.{ *, given }

def embed(title: String, cssModule: String, modules: EsmList = Nil)(body: Modifier*)(using
    ctx: EmbedContext
) =
  frag(
    page.ui.doctype,
    page.ui.htmlTag(using ctx.lang)(
      cls := ctx.bg,
      head(
        page.ui.charset,
        page.ui.viewport,
        page.ui.metaCsp(basicCsp.withNonce(ctx.nonce).withInlineIconFont),
        st.headTitle(title),
        (ctx.bg == "system").option(page.ui.systemThemeScript(ctx.nonce.some)),
        page.ui.pieceSprite(ctx.pieceSet.name),
        cssTag("theme-light"), // includes both light & dark colors
        cssTag(cssModule),
        page.ui.scriptsPreload(modules.flatMap(_.map(_.key)))
      ),
      st.body(
        page.ui.dataSoundSet := lila.pref.SoundSet.silent.key,
        page.ui.dataAssetUrl,
        page.ui.dataAssetVersion := assetVersion.value,
        page.ui.dataTheme        := ctx.bg,
        page.ui.dataPieceSet     := ctx.pieceSet.name,
        page.ui.dataBoard        := ctx.boardClass,
        page.ui.dataDev,
        body
      )
    )
  )
