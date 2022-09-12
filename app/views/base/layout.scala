package views.html.base

import controllers.routes
import play.api.i18n.Lang

import lila.api.{ AnnounceStore, Context }
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.common.{ ContentSecurityPolicy, Nonce }
import lila.common.base.StringUtils.escapeHtmlRaw

object layout {

  object bits {
    val doctype                      = raw("<!DOCTYPE html>")
    def htmlTag(implicit lang: Lang) = html(st.lang := lang.code, dir := isRTL.option("rtl"))
    val topComment = raw("""<!-- Lichess is open source! See https://lichess.org/source -->""")
    val charset    = raw("""<meta charset="utf-8">""")
    val viewport = raw(
      """<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">"""
    )
    def metaCsp(csp: ContentSecurityPolicy): Frag =
      raw {
        s"""<meta http-equiv="Content-Security-Policy" content="$csp">"""
      }
    def metaCsp(csp: Option[ContentSecurityPolicy])(implicit ctx: Context): Frag =
      metaCsp(csp getOrElse defaultCsp)
    def metaThemeColor(implicit ctx: Context): Frag =
      raw {
        s"""<meta name="theme-color" content="${ctx.pref.themeColor}">"""
      }
    def pieceSprite(implicit ctx: Context): Frag = pieceSprite(ctx.currentPieceSet)
    def pieceSprite(ps: lila.pref.PieceSet): Frag =
      link(
        id   := "piece-sprite",
        href := assetUrl(s"piece-css/$ps.${env.pieceImageExternal.get() ?? "external."}css"),
        rel  := "stylesheet"
      )
  }
  import bits._

  private val noTranslate = raw("""<meta name="google" content="notranslate">""")

  private def preload(href: String, as: String, crossorigin: Boolean, tpe: Option[String] = None) =
    raw(s"""<link rel="preload" href="$href" as="$as" ${tpe.??(t =>
        s"""type="$t" """
      )}${crossorigin ?? "crossorigin"}>""")

  private def fontPreload(implicit ctx: Context) = frag(
    preload(assetUrl("font/lichess.woff2"), "font", crossorigin = true, "font/woff2".some),
    preload(
      assetUrl("font/noto-sans-v14-latin-regular.woff2"),
      "font",
      crossorigin = true,
      "font/woff2".some
    ),
    !ctx.pref.pieceNotationIsLetter option
      preload(assetUrl("font/lichess.chess.woff2"), "font", crossorigin = true, "font/woff2".some)
  )
  private def boardPreload(implicit ctx: Context) = frag(
    preload(assetUrl(s"images/board/${ctx.currentTheme.file}"), "image", crossorigin = false),
    ctx.pref.is3d option
      preload(s"images/staunton/board/${ctx.currentTheme3d.file}", "image", crossorigin = false)
  )
  private def piecesPreload(implicit ctx: Context) =
    env.pieceImageExternal.get() option raw {
      (for {
        c <- List('w', 'b')
        p <- List('K', 'Q', 'R', 'B', 'N', 'P')
        href = staticAssetUrl(s"piece/${ctx.currentPieceSet.name}/$c$p.svg")
      } yield s"""<link rel="preload" href="$href" as="image">""").mkString
    }

  private val manifests = raw(
    """<link rel="manifest" href="/manifest.json"><meta name="twitter:site" content="@lichess">"""
  )

  private val jsLicense = raw("""<link rel="jslicense" href="/source">""")

  private val favicons = raw {
    List(512, 256, 192, 128, 64)
      .map { px =>
        s"""<link rel="icon" type="image/png" href="${assetUrl(
            s"logo/lichess-favicon-$px.png"
          )}" sizes="${px}x$px">"""
      }
      .mkString(
        "",
        "",
        s"""<link id="favicon" rel="icon" type="image/png" href="${assetUrl(
            "logo/lichess-favicon-32.png"
          )}" sizes="32x32">"""
      )
  }
  private def blindModeForm(implicit ctx: Context) =
    raw(s"""<form id="blind-mode" action="${routes.Main.toggleBlindMode}" method="POST"><input type="hidden" name="enable" value="${if (
        ctx.blind
      )
        0
      else
        1}"><input type="hidden" name="redirect" value="${ctx.req.path}"><button type="submit">Accessibility: ${if (
        ctx.blind
      )
        "Disable"
      else "Enable"} blind mode</button></form>""")

  private def zenToggle(implicit ctx: Context) =
    spaceless(s"""
  <a data-icon="" id="zentog" class="text fbt active">
    ${trans.preferences.zenMode.txt()}
  </a>""")

  private def dasher(me: lila.user.User) =
    div(cls := "dasher")(
      a(id := "user_tag", cls := "toggle link", href := routes.Auth.logoutGet)(me.username),
      div(id := "dasher_app", cls := "dropdown")
    )

  private def allNotifications(implicit ctx: Context) =
    spaceless(s"""<div>
  <a id="challenge-toggle" class="toggle link">
    <span title="${trans.challenge.challenges
        .txt()}" class="data-count" data-count="${ctx.nbChallenges}" data-icon=""></span>
  </a>
  <div id="challenge-app" class="dropdown"></div>
</div>
<div>
  <a id="notify-toggle" class="toggle link">
    <span title="${trans.notifications
        .txt()}" class="data-count" data-count="${ctx.nbNotifications}" data-icon=""></span>
  </a>
  <div id="notify-app" class="dropdown"></div>
</div>""")

  private def anonDasher(implicit ctx: Context) =
    spaceless(s"""<div class="dasher">
  <a class="toggle link anon">
    <span title="${trans.preferences.preferences.txt()}" data-icon=""></span>
  </a>
  <div id="dasher_app" class="dropdown"></div>
</div>
<a href="${routes.Auth.login}?referrer=${ctx.req.path}" class="signin button button-empty">${trans.signIn
        .txt()}</a>""")

  private val clinputLink = a(cls := "link")(span(dataIcon := ""))

  private def clinput(implicit ctx: Context) =
    div(id := "clinput")(
      clinputLink,
      input(
        spellcheck   := "false",
        autocomplete := ctx.blind.toString,
        aria.label   := trans.search.search.txt(),
        placeholder  := trans.search.search.txt()
      )
    )

  private def current2dTheme(implicit ctx: Context) =
    if (ctx.pref.is3d && ctx.pref.theme == "horsey") lila.pref.Theme.default
    else ctx.currentTheme

  private def botImage =
    img(
      src   := assetUrl("images/icons/bot.png"),
      title := "Robot chess",
      style :=
        "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;vertical-align:text-top"
    )

  private def loadScripts(moreJs: Frag, chessground: Boolean)(implicit ctx: Context) =
    frag(
      chessground option chessgroundTag,
      ctx.requiresFingerprint option fingerprintTag,
      ctx.nonce map inlineJs.apply,
      if (netConfig.minifiedAssets)
        jsModule("lichess")
      else
        frag(
          depsTag,
          jsModule("site")
        ),
      moreJs,
      ctx.pageData.inquiry.isDefined option jsModule("mod.inquiry")
    )

  private def hrefLang(lang: String, path: String) =
    s"""<link rel="alternate" hreflang="$lang" href="$netBaseUrl/$path"/>"""

  private def hrefLangs(path: String)(implicit ctx: Context) = raw {
    hrefLang("x-default", path) + hrefLang("en", path) +
      lila.i18n.LangList.popularAlternateLanguageCodes.map { lang =>
        hrefLang(lang, s"$lang$path")
      }.mkString
  }

  private val spinnerMask = raw(
    """<svg width="0" height="0"><mask id="mask"><path fill="#fff" stroke="#fff" stroke-linejoin="round" d="M38.956.5c-3.53.418-6.452.902-9.286 2.984C5.534 1.786-.692 18.533.68 29.364 3.493 50.214 31.918 55.785 41.329 41.7c-7.444 7.696-19.276 8.752-28.323 3.084C3.959 39.116-.506 27.392 4.683 17.567 9.873 7.742 18.996 4.535 29.03 6.405c2.43-1.418 5.225-3.22 7.655-3.187l-1.694 4.86 12.752 21.37c-.439 5.654-5.459 6.112-5.459 6.112-.574-1.47-1.634-2.942-4.842-6.036-3.207-3.094-17.465-10.177-15.788-16.207-2.001 6.967 10.311 14.152 14.04 17.663 3.73 3.51 5.426 6.04 5.795 6.756 0 0 9.392-2.504 7.838-8.927L37.4 7.171z"/></mask></svg>"""
  )

  private val spaceRegex              = """\s{2,}+""".r
  private def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  private val dataVapid         = attr("data-vapid")
  private val dataUser          = attr("data-user")
  private val dataSocketDomains = attr("data-socket-domains") := netConfig.socketDomains.mkString(",")
  private val dataNonce         = attr("data-nonce")
  private val dataAnnounce      = attr("data-announce")
  val dataSoundSet              = attr("data-sound-set")
  val dataTheme                 = attr("data-theme")
  val dataDirection             = attr("data-direction")
  val dataPieceSet              = attr("data-piece-set")
  val dataAssetUrl              = attr("data-asset-url")      := netConfig.assetBaseUrl.value
  val dataAssetVersion          = attr("data-asset-version")
  val dataDev                   = attr("data-dev")            := (!netConfig.minifiedAssets).option("true")

  def apply(
      title: String,
      fullTitle: Option[String] = None,
      robots: Boolean = netConfig.crawlable,
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag,
      playing: Boolean = false,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      chessground: Boolean = true,
      zoomable: Boolean = false,
      zenable: Boolean = false,
      csp: Option[ContentSecurityPolicy] = None,
      wrapClass: String = "",
      atomLinkTag: Option[Tag] = None,
      withHrefLangs: Option[String] = None
  )(body: Frag)(implicit ctx: Context): Frag =
    frag(
      doctype,
      htmlTag(ctx.lang)(
        topComment,
        head(
          charset,
          viewport,
          metaCsp(csp),
          metaThemeColor,
          st.headTitle {
            if (ctx.blind) "lichess"
            else if (netConfig.isProd) fullTitle | s"$title • lichess.org"
            else s"[dev] ${fullTitle | s"$title • lichess.dev"}"
          },
          cssTag("site"),
          ctx.pref.is3d option cssTag("board-3d"),
          ctx.pageData.inquiry.isDefined option cssTagNoTheme("mod.inquiry"),
          ctx.userContext.impersonatedBy.isDefined option cssTagNoTheme("mod.impersonate"),
          ctx.blind option cssTagNoTheme("blind"),
          moreCss,
          pieceSprite,
          meta(
            content := openGraph.fold(trans.siteDescription.txt())(o => o.description),
            name    := "description"
          ),
          link(rel := "mask-icon", href := assetUrl("logo/lichess.svg"), attr("color") := "black"),
          favicons,
          !robots option raw("""<meta content="noindex, nofollow" name="robots">"""),
          noTranslate,
          openGraph.map(_.frags),
          (atomLinkTag | link(
            href     := routes.Blog.atom,
            st.title := trans.blog.txt()
          ))(
            tpe := "application/atom+xml",
            rel := "alternate"
          ),
          ctx.currentBg == "transp" option ctx.pref.bgImgOrDefault map { img =>
            raw(
              s"""<style id="bg-data">body.transp::before{background-image:url("${escapeHtmlRaw(img)
                  .replace("&amp;", "&")}");}</style>"""
            )
          },
          fontPreload,
          boardPreload,
          piecesPreload,
          manifests,
          jsLicense,
          withHrefLangs.map(hrefLangs)
        ),
        st.body(
          cls := {
            val baseClass =
              s"${ctx.currentBg} ${current2dTheme.cssClass} ${ctx.currentTheme3d.cssClass} ${ctx.currentPieceSet3d.toString} coords-${ctx.pref.coordsClass}"
            List(
              baseClass              -> true,
              "dark-board"           -> (ctx.pref.bg == lila.pref.Pref.Bg.DARKBOARD),
              "piece-letter"         -> ctx.pref.pieceNotationIsLetter,
              "zen"                  -> ctx.pref.isZen,
              "blind-mode"           -> ctx.blind,
              "kid"                  -> ctx.kid,
              "mobile"               -> ctx.isMobileBrowser,
              "playing fixed-scroll" -> playing,
              "zenable"              -> zenable,
              "no-rating"            -> !ctx.pref.showRatings
            )
          },
          dataDev,
          dataVapid    := vapidPublicKey,
          dataUser     := ctx.userId,
          dataSoundSet := ctx.currentSoundSet.toString,
          dataSocketDomains,
          dataAssetUrl,
          dataAssetVersion := assetVersion.value,
          dataNonce        := ctx.nonce.ifTrue(sameAssetDomain).map(_.value),
          dataTheme        := ctx.currentBg,
          dataPieceSet     := ctx.currentPieceSet.name,
          dataAnnounce     := AnnounceStore.get.map(a => safeJsonValue(a.json)),
          style            := zoomable option s"--zoom:${ctx.zoom}"
        )(
          blindModeForm,
          ctx.pageData.inquiry map { views.html.mod.inquiry(_) },
          ctx.me ifTrue ctx.userContext.impersonatedBy.isDefined map { views.html.mod.impersonate(_) },
          netConfig.stageBanner option views.html.base.bits.stage,
          lila.security.EmailConfirm.cookie
            .get(ctx.req)
            .ifTrue(ctx.isAnon)
            .map(views.html.auth.bits.checkYourEmailBanner(_)),
          zenable option zenToggle,
          siteHeader.apply,
          div(
            id := "main-wrap",
            cls := List(
              wrapClass -> wrapClass.nonEmpty,
              "is2d"    -> ctx.pref.is2d,
              "is3d"    -> ctx.pref.is3d
            )
          )(body),
          ctx.me.exists(_.enabled) option div(id := "friend_box")(
            div(cls := "friend_box_title")(trans.nbFriendsOnline.plural(0, iconTag(""))),
            div(cls   := "content_wrap none")(
              div(cls := "content list")
            )
          ),
          netConfig.socketDomains.nonEmpty option a(
            id       := "reconnecting",
            cls      := "link text",
            dataIcon := ""
          )(trans.reconnecting()),
          ctx.pref.agreementNeededSince map { date =>
            div(id := "agreement")(
              div(
                "Lichess has updated the ",
                a(href := routes.Page.tos)("Terms of Service"),
                " as of ",
                showDate(date),
                "."
              ),
              postForm(action := routes.Pref.set("agreement"))(
                button(cls := "button")("OK")
              )
            )
          },
          spinnerMask,
          loadScripts(moreJs, chessground)
        )
      )
    )

  object siteHeader {

    private val topnavToggle = spaceless(
      """
<input type="checkbox" id="tn-tg" class="topnav-toggle fullscreen-toggle" autocomplete="off" aria-label="Navigation">
<label for="tn-tg" class="fullscreen-mask"></label>
<label for="tn-tg" class="hbg"><span class="hbg__in"></span></label>"""
    )

    private def reports(implicit ctx: Context) =
      if (isGranted(_.SeeReport)) {
        blockingReportScores match {
          case (score, mid, high) =>
            a(
              cls := List(
                "link data-count report-score link-center" -> true,
                "report-score--high"                       -> (score > high),
                "report-score--low"                        -> (score <= mid)
              ),
              title     := "Moderation",
              href      := routes.Report.list,
              dataCount := score,
              dataIcon  := ""
            )
        }
      }.some
      else
        (isGranted(_.PublicChatView)) option
          a(
            cls      := "link",
            title    := "Moderation",
            href     := routes.Mod.publicChat,
            dataIcon := ""
          )

    private def teamRequests(implicit ctx: Context) =
      ctx.teamNbRequests > 0 option
        a(
          cls       := "link data-count link-center",
          href      := routes.Team.requests,
          dataCount := ctx.teamNbRequests,
          dataIcon  := "",
          title     := trans.team.teams.txt()
        )

    def apply(implicit ctx: Context) =
      header(id := "top")(
        div(cls := "site-title-nav")(
          !ctx.isAppealUser option topnavToggle,
          h1(cls := "site-title")(
            if (ctx.kid) span(title := trans.kidMode.txt(), cls := "kiddo")(":)")
            else ctx.isBot option botImage,
            a(href := "/")(
              "lichess",
              span(if (netConfig.isProd) ".org" else ".dev")
            )
          ),
          ctx.blind option h2("Navigation"),
          !ctx.isAppealUser option topnav()
        ),
        div(cls := "site-buttons")(
          !ctx.isAppealUser option clinput,
          reports,
          teamRequests,
          if (ctx.isAppealUser)
            postForm(action := routes.Auth.logout)(
              submitButton(cls := "button button-red link")(trans.logOut())
            )
          else
            ctx.me map { me =>
              frag(allNotifications, dasher(me))
            } getOrElse { !ctx.pageData.error option anonDasher }
        )
      )
  }

  object inlineJs {

    private val i18nKeys = List(
      trans.pause.key,
      trans.resume.key,
      trans.nbFriendsOnline.key,
      trans.timeago.justNow.key,
      trans.timeago.inNbSeconds.key,
      trans.timeago.inNbMinutes.key,
      trans.timeago.inNbHours.key,
      trans.timeago.inNbDays.key,
      trans.timeago.inNbWeeks.key,
      trans.timeago.inNbMonths.key,
      trans.timeago.inNbYears.key,
      trans.timeago.rightNow.key,
      trans.timeago.nbSecondsAgo.key,
      trans.timeago.nbMinutesAgo.key,
      trans.timeago.nbHoursAgo.key,
      trans.timeago.nbDaysAgo.key,
      trans.timeago.nbWeeksAgo.key,
      trans.timeago.nbMonthsAgo.key,
      trans.timeago.nbYearsAgo.key
    )

    private val cache = scala.collection.mutable.AnyRefMap.empty[Lang, String]

    private def jsCode(implicit lang: Lang) =
      cache.getOrElseUpdate(
        lang,
        s"""lichess={load:new Promise(r=>document.addEventListener("DOMContentLoaded",r)),quantity:${lila.i18n
            .JsQuantity(lang)},siteI18n:${safeJsonValue(i18nJsObject(i18nKeys))}}"""
      )

    def apply(nonce: Nonce)(implicit lang: Lang) =
      embedJsUnsafe(jsCode, nonce)
  }
}
