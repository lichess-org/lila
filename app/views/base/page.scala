package views.base

import scalalib.StringUtils.escapeHtmlRaw

import lila.app.UiEnv.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.ui.{ RenderedPage, PageFlags }

object page:

  val pieceSetImages = lila.web.ui.PieceSetImages(assetHelper)

  val ui = lila.web.ui.layout(helpers, assetHelper)(
    popularAlternateLanguages = lila.i18n.LangList.popularAlternateLanguages,
    reportScoreThreshold = env.report.scoreThresholdsSetting.get,
    reportScore = () => env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0)
  )
  import ui.*

  private val topnav = lila.web.ui.TopNav(helpers)

  private def metaThemeColor(using ctx: Context): Frag =
    raw(s"""<meta name="theme-color" content="${ctx.pref.themeColor}">""")

  private def boardPreload(using ctx: Context) = frag(
    imagePreload(assetUrl(s"images/board/${ctx.pref.currentTheme.file}")),
    ctx.pref.is3d.option:
      imagePreload(assetUrl(s"images/staunton/board/${ctx.pref.currentTheme3d.file}"))
  )

  def boardStyle(zoomable: Boolean)(using ctx: Context) =
    s"---board-opacity:${ctx.pref.board.opacity};" +
      s"---board-brightness:${ctx.pref.board.brightness};" +
      s"---board-hue:${ctx.pref.board.hue};" +
      zoomable.so(s"---zoom:$pageZoom;")

  def apply(p: Page)(using ctx: PageContext): RenderedPage =
    import ctx.pref
    val allModules = p.modules ++
      p.pageModule.so(module => esmPage(module.name)) ++
      ctx.needsFp.so(fingerprintTag)
    val zenable = p.flags(PageFlags.zen)
    val playing = p.flags(PageFlags.playing)
    val pageFrag = frag(
      doctype,
      htmlTag(
        (ctx.impersonatedBy.isEmpty && !ctx.blind)
          .option(cls := ctx.pref.themeColorClass),
        topComment,
        head(
          charset,
          viewport,
          metaCsp(p.csp.map(_(defaultCsp))),
          metaThemeColor,
          st.headTitle:
            val prodTitle = p.fullTitle | s"${p.title} • $siteName"
            if env.mode.isProd then prodTitle
            else s"${ctx.me.so(_.username.value + " ")} $prodTitle"
          ,
          cssTag("lib.theme.all"),
          cssTag("site"),
          pref.is3d.option(cssTag("lib.board-3d")),
          ctx.data.inquiry.isDefined.option(cssTag("mod.inquiry")),
          ctx.impersonatedBy.isDefined.option(cssTag("mod.impersonate")),
          ctx.blind.option(cssTag("bits.blind")),
          p.cssKeys.map(cssTag),
          meta(
            content := p.openGraph.fold(trans.site.siteDescription.txt())(o => o.description),
            name := "description"
          ),
          link(rel := "mask-icon", href := staticAssetUrl("logo/lichess.svg"), attr("color") := "black"),
          favicons,
          (p.flags(PageFlags.noRobots) || !netConfig.crawlable).option:
            raw("""<meta content="noindex, nofollow" name="robots">""")
          ,
          noTranslate,
          p.openGraph.map(lila.web.ui.openGraph),
          p.atomLinkTag | dailyNewsAtom,
          (pref.bg == lila.pref.Pref.Bg.TRANSPARENT).option(pref.bgImgOrDefault).map { loc =>
            val url =
              if loc.startsWith("/assets/") then assetUrl(loc.drop(8))
              else escapeHtmlRaw(loc).replace("&amp;", "&")
            raw(s"""<style id="bg-data">html.transp::before{background-image:url("$url");}</style>""")
          },
          fontsPreload,
          boardPreload,
          manifests,
          p.withHrefLangs.map(hrefLangs),
          sitePreload(p.i18nModules, ctx.data.inquiry.isDefined.option(Esm("mod.inquiry")) :: allModules),
          lichessFontFaceCss,
          pieceSetImages.load(ctx.pref.currentPieceSet.name),
          (ctx.pref.bg === lila.pref.Pref.Bg.SYSTEM || ctx.impersonatedBy.isDefined)
            .so(systemThemeScript(ctx.nonce))
        ).pipe(p.transformHead),
        st.body(
          cls := {
            val baseClass = s"${pref.currentBg} coords-${pref.coordsClass}"
            List(
              baseClass -> true,
              "simple-board" -> pref.simpleBoard,
              "piece-letter" -> pref.pieceNotationIsLetter,
              "blind-mode" -> ctx.blind,
              "kid" -> ctx.kid.yes,
              "mobile" -> lila.common.HTTPRequest.isMobileBrowser(ctx.req),
              "playing fixed-scroll" -> playing,
              "no-rating" -> (!pref.showRatings || (playing && pref.hideRatingsInGame)),
              "no-flair" -> !pref.flairs,
              "zen" -> (pref.isZen || (playing && pref.isZenAuto)),
              "zenable" -> zenable,
              "zen-auto" -> (zenable && pref.isZenAuto)
            )
          },
          dataVapid := (ctx.isAuth && env.security.lilaCookie.isRememberMe(ctx.req))
            .option(env.push.vapidPublicKey),
          dataUser := ctx.userId,
          dataUsername := ctx.username,
          dataSoundSet := pref.currentSoundSet.toString,
          attr("data-socket-domains") := (if ~pref.usingAltSocket then netConfig.socketAlts
                                          else netConfig.socketDomains).mkString(","),
          dataAssetUrl,
          dataAssetVersion := assetVersion,
          dataNonce := ctx.nonce,
          dataTheme := pref.currentBg,
          dataBoard := pref.currentTheme.name,
          dataPieceSet := pref.currentPieceSet.name,
          dataBoard3d := pref.currentTheme3d.name,
          dataPieceSet3d := pref.currentPieceSet3d.name,
          dataAnnounce := lila.web.AnnounceApi.get.map(a => safeJsonValue(a.json)),
          attr("data-i18n-catalog") := assetHelper.manifest
            .js(s"i18n/${ctx.lang.code}")
            .map(name => staticAssetUrl(s"compiled/$name")),
          style := boardStyle(p.flags(PageFlags.zoom))
        )(
          blindModeForm,
          for in <- ctx.data.inquiry; me <- ctx.me yield views.mod.inquiryUi(in)(using ctx, me),
          ctx.me.ifTrue(ctx.impersonatedBy.isDefined).map { views.mod.ui.impersonate(_) },
          netConfig.stageBanner.option(views.bits.stage),
          ctx.isAnon
            .so(lila.security.EmailConfirm.cookie.get(ctx.req))
            .map(u =>
              frag(cssTag("bits.email-confirm"), views.auth.checkYourEmailBanner(u.username, u.email))
            ),
          zenable.option(zenZone),
          Option.unless(p.flags(PageFlags.noHeader)):
            ui.siteHeader(
              zenable = zenable,
              isAppealUser = ctx.isAppealUser,
              challenges = ctx.nbChallenges,
              notifications = ctx.nbNotifications.value,
              error = ctx.data.error,
              topnav = topnav(
                hasClas = ctx.hasClas,
                hasDgt = ctx.pref.hasDgt
              )
            )
          ,
          div(
            id := "main-wrap",
            cls := List(
              "full-screen-force" -> p.flags(PageFlags.fullScreen),
              "is2d" -> pref.is2d,
              "is3d" -> pref.is3d
            )
          )(p.transform(p.body)),
          bottomHtml,
          ctx.nonce.map(inlineJs(_, allModules)),
          modulesInit(allModules, ctx.nonce),
          p.pageModule.map { mod => frag(jsonScript(mod.data)) }
        )
      )
    )
    RenderedPage(pageFrag.render)
