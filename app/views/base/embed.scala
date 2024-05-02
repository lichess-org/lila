package views.base

import lila.app.UiEnv.{ *, given }

def embed(title: String, cssModule: String, modules: EsmList = Nil)(body: Modifier*)(using
    ctx: EmbedContext
) = lila.ui.Snippet:
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
        page.ui.modulesPreload(modules, isInquiry = false)
      ),
      st.body(cls := s"highlight ${ctx.boardClass}")(
        page.ui.dataSoundSet := lila.pref.SoundSet.silent.key,
        page.ui.dataAssetUrl,
        page.ui.dataAssetVersion := assetVersion.value,
        page.ui.dataTheme        := ctx.bg,
        page.ui.dataPieceSet     := ctx.pieceSet.name,
        page.ui.dataDev,
        body
      )
    )
  )
