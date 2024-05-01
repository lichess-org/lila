package views.base

import play.api.i18n.Lang

import lila.ui.ContentSecurityPolicy
import lila.app.templating.Environment.{ *, given }

import lila.common.String.html.safeJsonValue
import scalalib.StringUtils.escapeHtmlRaw

def page(p: Page)(using ctx: PageContext): Frag =
  layout(
    title = p.title,
    fullTitle = p.fullTitle,
    robots = p.robots | netConfig.crawlable,
    moreCss = p.cssFrag,
    modules = p.modules,
    moreJs = p.jsFrag.fold(emptyFrag)(_(ctx.nonce)),
    pageModule = p.pageModule,
    playing = p.playing,
    openGraph = p.openGraph,
    zoomable = p.zoomable,
    zenable = p.zenable,
    csp = p.csp.map(_(defaultCsp)),
    wrapClass = p.wrapClass,
    atomLinkTag = p.atomLinkTag,
    withHrefLangs = p.withHrefLangs
  )(p.transform(p.body))

object layout:

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
    raw:
      s"""<meta name="theme-color" media="(prefers-color-scheme: light)" content="${ctx.pref.themeColorLight}">""" +
        s"""<meta name="theme-color" media="(prefers-color-scheme: dark)" content="${ctx.pref.themeColorDark}">""" +
        s"""<meta name="theme-color" content="${ctx.pref.themeColor}">"""

  private def boardPreload(using ctx: Context) = frag(
    preload(assetUrl(s"images/board/${ctx.pref.currentTheme.file}"), "image", crossorigin = false),
    ctx.pref.is3d.option(
      preload(
        assetUrl(s"images/staunton/board/${ctx.pref.currentTheme3d.file}"),
        "image",
        crossorigin = false
      )
    )
  )

  private def current2dTheme(using ctx: Context) =
    if ctx.pref.is3d && ctx.pref.theme == "horsey" then lila.pref.Theme.default
    else ctx.pref.currentTheme

  def boardStyle(zoomable: Boolean)(using ctx: Context) =
    s"---board-opacity:${ctx.pref.board.opacity};" +
      s"---board-brightness:${ctx.pref.board.brightness};" +
      s"---board-hue:${ctx.pref.board.hue};" +
      zoomable.so(s"---zoom:$pageZoom;")

  def apply(
      title: String,
      fullTitle: Option[String] = None,
      robots: Boolean = netConfig.crawlable,
      moreCss: Frag = emptyFrag,
      modules: EsmList = Nil,
      moreJs: Frag = emptyFrag,
      pageModule: Option[PageModule] = None,
      playing: Boolean = false,
      openGraph: Option[OpenGraph] = None,
      zoomable: Boolean = false,
      zenable: Boolean = false,
      csp: Option[ContentSecurityPolicy] = None,
      wrapClass: String = "",
      atomLinkTag: Option[Tag] = None,
      withHrefLangs: Option[lila.ui.LangPath] = None
  )(body: Frag)(using ctx: PageContext): Frag =
    import ctx.pref
    frag(
      doctype,
      htmlTag(
        (ctx.data.inquiry.isEmpty && ctx.impersonatedBy.isEmpty && !ctx.blind)
          .option(cls := ctx.pref.themeColorClass),
        topComment,
        head(
          charset,
          viewport,
          metaCsp(csp),
          metaThemeColor,
          st.headTitle:
            val prodTitle = fullTitle | s"$title • $siteName"
            if netConfig.isProd then prodTitle
            else s"${ctx.me.so(_.username + " ")} $prodTitle"
          ,
          cssTag("theme-all"),
          cssTag("site"),
          pref.is3d.option(cssTag("board-3d")),
          ctx.data.inquiry.isDefined.option(cssTag("mod.inquiry")),
          ctx.impersonatedBy.isDefined.option(cssTag("mod.impersonate")),
          ctx.blind.option(cssTag("blind")),
          moreCss,
          pieceSprite(ctx.pref.currentPieceSet.name),
          meta(
            content := openGraph.fold(trans.site.siteDescription.txt())(o => o.description),
            name    := "description"
          ),
          link(rel := "mask-icon", href := assetUrl("logo/lichess.svg"), attr("color") := "black"),
          favicons,
          (!robots).option(raw("""<meta content="noindex, nofollow" name="robots">""")),
          noTranslate,
          openGraph.map(lila.web.ui.openGraph),
          atomLinkTag | dailyNewsAtom,
          (pref.bg == lila.pref.Pref.Bg.TRANSPARENT).option(pref.bgImgOrDefault).map { img =>
            raw:
              s"""<style id="bg-data">html.transp::before{background-image:url("${escapeHtmlRaw(img)
                  .replace("&amp;", "&")}");}</style>"""
          },
          fontPreload,
          boardPreload,
          manifests,
          jsLicense,
          withHrefLangs.map(hrefLangs),
          modulesPreload(
            modules ++ pageModule.so(module => jsPageModule(module.name)),
            isInquiry = ctx.data.inquiry.isDefined
          ),
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
              "playing fixed-scroll" -> playing,
              "no-rating"            -> !pref.showRatings,
              "no-flair"             -> !pref.flairs,
              "zen"                  -> (pref.isZen || (playing && pref.isZenAuto)),
              "zenable"              -> zenable,
              "zen-auto"             -> (zenable && pref.isZenAuto)
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
          style            := boardStyle(zoomable)
        )(
          blindModeForm,
          ctx.data.inquiry.map { views.mod.inquiry(_) },
          ctx.me.ifTrue(ctx.impersonatedBy.isDefined).map { views.mod.ui.impersonate(_) },
          netConfig.stageBanner.option(views.bits.stage),
          lila.security.EmailConfirm.cookie
            .get(ctx.req)
            .ifTrue(ctx.isAnon)
            .map(u => views.auth.checkYourEmailBanner(u.username, u.email)),
          zenable.option(zenZone),
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
          ),
          div(
            id := "main-wrap",
            cls := List(
              wrapClass -> wrapClass.nonEmpty,
              "is2d"    -> pref.is2d,
              "is3d"    -> pref.is3d
            )
          )(body),
          bottomHtml,
          div(id := "inline-scripts")(
            frag(ctx.needsFp.option(views.auth.fingerprintTag), ctx.nonce.map(inlineJs.apply)),
            modulesInit(modules ++ pageModule.so(module => jsPageModule(module.name))),
            moreJs,
            pageModule.map { mod => frag(jsonScript(mod.data)) }
          )
        )
      )
    )
