package views.base

import lila.app.UiEnv.{ *, given }
import lila.ui.ContentSecurityPolicy

def embed(
    title: String,
    cssModule: String,
    modules: EsmList = Nil,
    pageModule: Option[PageModule] = None,
    csp: Update[ContentSecurityPolicy] = identity
)(body: Modifier*)(using
    ctx: EmbedContext
) = lila.ui.Snippet:
  val allModules = modules ++ pageModule.so(module => jsPageModule(module.name))
  frag(
    page.ui.doctype,
    page.ui.htmlTag(using ctx.lang)(
      cls := ctx.bg,
      head(
        page.ui.charset,
        page.ui.viewport,
        page.ui.metaCsp(csp(basicCsp.withNonce(ctx.nonce).withInlineIconFont)),
        st.headTitle(title),
        (ctx.bg == "system").option(page.ui.systemThemeScript(ctx.nonce.some)),
        page.ui.pieceSprite(ctx.pieceSet.name),
        cssTag("common.theme.embed"), // includes both light & dark colors
        cssTag(cssModule),
        page.ui.scriptsPreload(allModules.flatMap(_.map(_.key))),
        modules.flatMap(_.map(_.init(ctx.nonce.some))), // in body
        pageModule.map { mod => frag(jsonScript(mod.data)) }
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
