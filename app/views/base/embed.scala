package views.base

import lila.app.UiEnv.{ *, given }
import lila.ui.ContentSecurityPolicy

object embed:
  /* a minimalist embed that doesn't load site.ts */
  def minimal(title: String, cssKeys: List[String] = Nil, modules: EsmList = Nil)(body: Modifier*)(using
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
          cssTag("common.theme.embed"), // includes both light & dark colors
          cssKeys.map(cssTag),
          page.ui.scriptsPreload(modules.flatMap(_.map(_.key)))
        ),
        st.body(
          bodyModifiers,
          body
        )
      )
    )

  private def bodyModifiers(using ctx: EmbedContext) = List(
    cls                  := List("simple-board" -> ctx.pref.simpleBoard),
    page.ui.dataSoundSet := lila.pref.SoundSet.silent.key,
    page.ui.dataAssetUrl,
    page.ui.dataAssetVersion := assetVersion.value,
    page.ui.dataTheme        := ctx.bg,
    page.ui.dataPieceSet     := ctx.pieceSet.name,
    page.ui.dataBoard        := ctx.boardClass,
    page.ui.dataDev,
    page.ui.dataSocketDomains,
    style := page.boardStyle(zoomable = false)
  )

  /* a heavier embed that loads site.ts and connects to WS */
  def site(
      title: String,
      cssKeys: List[String] = Nil,
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
          cssKeys.map(cssTag),
          page.ui.sitePreload(allModules, isInquiry = false),
          page.ui.lichessFontFaceCss
        ),
        st.body(bodyModifiers)(
          body,
          page.ui.modulesInit(allModules, ctx.nonce.some),
          pageModule.map { mod => frag(jsonScript(mod.data)) }
        )
      )
    )
