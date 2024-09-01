package views.base
import scalalib.StringUtils.escapeHtmlRaw

import lila.app.UiEnv.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.ui.RenderedPage

object page:

  val ui = lila.web.ui.layout(helpers, assetHelper)(
    jsQuantity = lila.i18n.JsQuantity.apply,
    isRTL = lila.i18n.LangList.isRTL,
    popularAlternateLanguages = lila.i18n.LangList.popularAlternateLanguages,
    reportScoreThreshold = env.report.scoreThresholdsSetting.get,
    reportScore = () => env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0)
  )
  import ui.*

  private val topnav = lila.web.ui.TopNav(helpers)

  private def metaThemeColor(using ctx: Context): Frag =
    raw(s"""<meta name="theme-color" content="${ctx.pref.themeColor}">""")

  private def boardPreload(using ctx: Context) = frag(
    preload(staticAssetUrl(s"images/board/${ctx.pref.currentTheme.file}"), "image", crossorigin = false),
    ctx.pref.is3d.option(
      preload(
        staticAssetUrl(s"images/staunton/board/${ctx.pref.currentTheme3d.file}"),
        "image",
        crossorigin = false
      )
    )
  )

  def boardStyle(zoomable: Boolean)(using ctx: Context) =
    s"---board-opacity:${ctx.pref.board.opacity};" +
      s"---board-brightness:${ctx.pref.board.brightness};" +
      s"---board-hue:${ctx.pref.board.hue};" +
      zoomable.so(s"---zoom:$pageZoom;")

  def apply(p: Page)(using ctx: PageContext): RenderedPage =
    import ctx.pref
    val allModules = p.modules ++ p.pageModule.so(module => jsPageModule(module.name))
    val pageFrag = frag(
      doctype,
      htmlTag(
        (ctx.data.inquiry.isEmpty && ctx.impersonatedBy.isEmpty && !ctx.blind)
          .option(cls := ctx.pref.themeColorClass),
        topComment,
        head(
          charset,
          viewport,
          metaCsp(p.csp.map(_(defaultCsp))),
          metaThemeColor,
          st.headTitle:
            val prodTitle = p.fullTitle | s"${p.title} • $siteName"
            if netConfig.isProd then prodTitle
            else s"${ctx.me.so(_.username.value + " ")} $prodTitle"
          ,
          cssTag("common.theme.all"),
          cssTag("site"),
          pref.is3d.option(cssTag("common.board-3d")),
          ctx.data.inquiry.isDefined.option(cssTag("mod.inquiry")),
          ctx.impersonatedBy.isDefined.option(cssTag("mod.impersonate")),
          ctx.blind.option(cssTag("bits.blind")),
          p.cssKeys.map(cssTag),
          pieceSprite(ctx.pref.currentPieceSet.name),
          meta(
            content := p.openGraph.fold(trans.site.siteDescription.txt())(o => o.description),
            name    := "description"
          ),
          link(rel := "mask-icon", href := staticAssetUrl("logo/lichess.svg"), attr("color") := "black"),
          favicons,
          (!p.robots || !netConfig.crawlable).option:
            raw("""<meta content="noindex, nofollow" name="robots">""")
          ,
          noTranslate,
          p.openGraph.map(lila.web.ui.openGraph),
          p.atomLinkTag | dailyNewsAtom,
          (pref.bg == lila.pref.Pref.Bg.TRANSPARENT).option(pref.bgImgOrDefault).map { img =>
            val url = escapeHtmlRaw(img).replace("&amp;", "&")
            raw(s"""<style id="bg-data">html.transp::before{background-image:url("$url");}</style>""")
          },
          fontPreload,
          boardPreload,
          manifests,
          p.withHrefLangs.map(hrefLangs),
          sitePreload(allModules, isInquiry = ctx.data.inquiry.isDefined),
          lichessFontFaceCss,
          (ctx.pref.bg === lila.pref.Pref.Bg.SYSTEM).so(systemThemeScript(ctx.nonce))
        ),
        st.body(
          cls := {
            val baseClass = s"${pref.currentBg} coords-${pref.coordsClass}"
            List(
              baseClass              -> true,
              "simple-board"         -> pref.simpleBoard,
              "piece-letter"         -> pref.pieceNotationIsLetter,
              "blind-mode"           -> ctx.blind,
              "kid"                  -> ctx.kid.yes,
              "mobile"               -> lila.common.HTTPRequest.isMobileBrowser(ctx.req),
              "playing fixed-scroll" -> p.playing,
              "no-rating"            -> !pref.showRatings,
              "no-flair"             -> !pref.flairs,
              "zen"                  -> (pref.isZen || (p.playing && pref.isZenAuto)),
              "zenable"              -> p.zenable,
              "zen-auto"             -> (p.zenable && pref.isZenAuto)
            )
          },
          dataDev,
          dataVapid := (ctx.isAuth && env.security.lilaCookie.isRememberMe(ctx.req))
            .option(env.push.vapidPublicKey),
          dataUser     := ctx.userId,
          dataSoundSet := pref.currentSoundSet.toString,
          dataSocketDomains,
          pref.isUsingAltSocket.option(dataSocketAlts),
          dataAssetUrl,
          dataAssetVersion := assetVersion,
          dataNonce        := ctx.nonce.ifTrue(sameAssetDomain).map(_.value),
          dataTheme        := pref.currentBg,
          dataBoard        := pref.currentTheme.name,
          dataPieceSet     := pref.currentPieceSet.name,
          dataBoard3d      := pref.currentTheme3d.name,
          dataPieceSet3d   := pref.currentPieceSet3d.name,
          dataAnnounce     := lila.web.AnnounceApi.get.map(a => safeJsonValue(a.json)),
          style            := boardStyle(p.zoomable)
        )(
          blindModeForm,
          ctx.data.inquiry.map { views.mod.inquiry(_) },
          ctx.me.ifTrue(ctx.impersonatedBy.isDefined).map { views.mod.ui.impersonate(_) },
          netConfig.stageBanner.option(views.bits.stage),
          lila.security.EmailConfirm.cookie
            .get(ctx.req)
            .ifTrue(ctx.isAnon)
            .map(u => views.auth.checkYourEmailBanner(u.username, u.email)),
          p.zenable.option(zenZone),
          ui.siteHeader(
            zenable = p.zenable,
            isAppealUser = ctx.isAppealUser,
            challenges = ctx.nbChallenges,
            notifications = ctx.nbNotifications.value,
            error = ctx.data.error,
            topnav = topnav(
              hasClas = ctx.hasClas,
              hasDgt = ctx.pref.hasDgt
            )
          ),
          div(
            id := "main-wrap",
            cls := List(
              "full-screen-force" -> p.fullScreenClass,
              "is2d"              -> pref.is2d,
              "is3d"              -> pref.is3d
            )
          )(p.transform(p.body)),
          bottomHtml,
          ctx.needsFp.option(views.auth.fingerprintTag),
          ctx.nonce.map(inlineJs.apply),
          modulesInit(allModules, ctx.nonce),
          p.jsFrag.fold(emptyFrag)(_(ctx.nonce)),
          p.pageModule.map { mod => frag(jsonScript(mod.data)) }
        )
      )
    )
    RenderedPage(pageFrag.render)
