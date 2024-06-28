package views.html.base

import play.api.i18n.Lang
import play.api.libs.json.Json

import org.joda.time.DateTime

import lila.api.{ AnnounceStore, Context }
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.ContentSecurityPolicy
import lila.common.String.html.safeJsonValue
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.common.CanonicalPath

import controllers.routes

object layout {

  object bits {
    val doctype                      = raw("<!DOCTYPE html>")
    def htmlTag(implicit lang: Lang) = html(st.lang := lila.i18n.languageCode(lang))
    val topComment = raw("""<!-- Lishogi is open source! See https://lishogi.org/source -->""")
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
        href := assetUrl(s"piece-css/$ps.css"),
        tpe  := "text/css",
        rel  := "stylesheet"
      )

    def chuPieceSprite(implicit ctx: Context): Frag = chuPieceSprite(ctx.currentChuPieceSet)
    def chuPieceSprite(ps: lila.pref.PieceSet): Frag =
      link(
        id   := "chu-piece-sprite",
        href := assetUrl(s"piece-css/$ps.css"),
        tpe  := "text/css",
        rel  := "stylesheet"
      )
    def kyoPieceSprite(implicit ctx: Context): Frag = kyoPieceSprite(ctx.currentKyoPieceSet)
    def kyoPieceSprite(ps: lila.pref.PieceSet): Frag =
      link(
        id   := "kyo-piece-sprite",
        href := assetUrl(s"piece-css/$ps.css"),
        tpe  := "text/css",
        rel  := "stylesheet"
      )
  }
  import bits._

  private val noTranslate = raw("""<meta name="google" content="notranslate">""")

  private def preload(href: String, as: String, crossorigin: Boolean, tpe: Option[String] = None) =
    raw(s"""<link rel="preload" href="$href" as="$as" ${tpe.??(t =>
        s"""type="$t" """
      )}${crossorigin ?? "crossorigin"}>""")

  private val fontPreload = frag(
    preload(assetUrl("font/lishogi.woff2"), "font", crossorigin = true, "font/woff2".some),
    preload(assetUrl("font/lishogi.shogi.woff2"), "font", crossorigin = true, "font/woff2".some)
  )

  private def boardPreload(implicit ctx: Context) =
    ctx.currentTheme.file map { file =>
      preload(assetUrl(s"images/boards/$file"), "image", crossorigin = false)
    }

  private val manifests = raw(
    """<link rel="manifest" href="/manifest.json"><meta name="twitter:site" content="@lishogi">"""
  )

  private val jsLicense = raw("""<link rel="jslicense" href="/source">""")

  private val favicons = raw {
    List(512, 256, 192, 128, 64)
      .map { px =>
        s"""<link rel="icon" type="image/png" href="${staticUrl(
            s"logo/lishogi-favicon-$px.png"
          )}" sizes="${px}x${px}">"""
      }
      .mkString(
        "",
        "",
        s"""<link id="favicon" rel="icon" type="image/png" href="${staticUrl(
            "logo/lishogi-favicon-32.png"
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
  private val zenToggle = raw("""<a data-icon="E" id="zentog" class="text fbt active">ZEN MODE</a>""")
  private def dasher(me: lila.user.User) =
    raw(
      s"""<div class="dasher"><a id="user_tag" class="toggle link">${me.username}</a><div id="dasher_app" class="dropdown"></div></div>"""
    )

  private def allNotifications(implicit ctx: Context) =
    spaceless(s"""<div>
  <a id="challenge-toggle" class="toggle link">
    <span title="${trans.challenges
        .txt()}" class="data-count" data-count="${ctx.nbChallenges}" data-icon="U"></span>
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

  private def anonDasher(playing: Boolean)(implicit ctx: Context) =
    spaceless(s"""<div class="dasher">
  <a class="toggle link anon">
    <span title="${trans.preferences.preferences.txt()}" data-icon="%"></span>
  </a>
  <div id="dasher_app" class="dropdown" data-playing="$playing"></div>
</div>
<a href="${langHref(
        s"${routes.Auth.login.url}?referrer=${ctx.req.path}"
      )}" class="signin button button-empty">${trans.signIn
        .txt()}</a>""")

  private val clinputLink = a(cls := "link")(span(dataIcon := "y"))

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

  private def switchLanguage(implicit ctx: Context) =
    spaceless(s"""<form method="post" action="/translation/select" class="header-langs">
    ${if (ctx.lang.language == "en")
        """<button type="submit" name="lang" value="ja-JP" title="ja-JP">日本語</button>"""
      else """<button type="submit" name="lang" value="en-US" title="en-US">English</button>"""}
    </form>""")

  private lazy val botImage = img(
    src   := staticUrl("images/icons/bot.png"),
    title := "Robot shogi",
    style :=
      "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;vertical-align:text-top"
  )

  private def canonical(canonicalPath: CanonicalPath)(implicit ctx: Context) = raw {
    val langQuery = ctx.req
      .getQueryString("lang")
      .flatMap(lila.i18n.I18nLangPicker.byQuery)
      .filterNot(_.language == "en")
      .fold("") { l =>
        s"?lang=${lila.i18n.languageCode(l)}"
      }
    s"""<link rel="canonical" href="$netBaseUrl${canonicalPath.value}$langQuery" />"""
  }

  private def hrefLang(langCode: String, pathWithQuery: String) =
    s"""<link rel="alternate" hreflang="$langCode" href="$netBaseUrl$pathWithQuery"/>"""

  private def defaultWithEnHrefLang(path: String) =
    hrefLang("x-default", path) + hrefLang("en", path)

  private def hrefLangs(altLangs: lila.i18n.LangList.AlternativeLangs)(implicit ctx: Context) = raw {
    val path = ctx.req.path
    altLangs match {
      case lila.i18n.LangList.EnglishJapanese =>
        defaultWithEnHrefLang(path) + hrefLang("ja", s"$path?lang=ja")
      case lila.i18n.LangList.All =>
        defaultWithEnHrefLang(path) + (lila.i18n.LangList.alternativeHrefLangCodes.map { langCode =>
          hrefLang(langCode, s"$path?lang=$langCode")
        }).mkString
      case lila.i18n.LangList.Custom(langPathMap) =>
        (langPathMap.map { case (langCode, path) =>
          if (langCode == "en") defaultWithEnHrefLang(path)
          else hrefLang(langCode, path)
        }).mkString
    }
  }

  private def cssBackgroundImageValue(url: String): String =
    if (url.isEmpty) "none" else s"url(${escapeHtmlRaw(url).replace("&amp;", "&")})"

  private def cssVariables(zoomable: Boolean)(implicit ctx: Context): Option[String] = {
    val zoom = zoomable option s"--zoom:${ctx.zoom};"
    val customBg = ctx.transpBgImg map { img =>
      s"--tr-bg-url:${cssBackgroundImageValue(img)};"
    }
    val customTheme = ctx.activeCustomTheme map { ct =>
      List(
        s"--c-board-color:${ct.boardColor};",
        s"--c-board-url:${cssBackgroundImageValue(ct.boardImg)};",
        s"--c-grid-color:${ct.gridColor};",
        s"--c-hands-color:${ct.handsColor};",
        s"--c-hands-url:${cssBackgroundImageValue(ct.handsImg)};"
      ).mkString("")
    }
    (zoom ++ customBg ++ customTheme).reduceLeftOption(_ + _)
  }

  private val spaceRegex              = """\s{2,}+""".r
  private def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  private val dataVapid         = attr("data-vapid")
  private val dataUser          = attr("data-user")
  private val dataDate          = attr("data-date")
  private val dataSocketDomains = attr("data-socket-domains")
  private val dataPreload       = attr("data-preload")
  private val dataNonce         = attr("data-nonce")
  private val dataAnnounce      = attr("data-announce")
  private val dataColorName     = attr("data-color-name")
  private val dataNotation      = attr("data-notation")
  val dataSoundSet              = attr("data-sound-set")
  val dataTheme                 = attr("data-theme")
  val dataPieceSet              = attr("data-piece-set")
  val dataChuPieceSet           = attr("data-chu-piece-set")
  val dataKyoPieceSet           = attr("data-kyo-piece-set")
  val dataAssetUrl              = attr("data-asset-url")
  val dataAssetVersion          = attr("data-asset-version")
  val dataDev                   = attr("data-dev")

  def apply(
      title: String,
      fullTitle: Option[String] = None,
      robots: Boolean = isGloballyCrawlable,
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag,
      playing: Boolean = false,
      openGraph: Option[lila.app.ui.OpenGraph] = None,
      shogiground: Boolean = true,
      zoomable: Boolean = false,
      deferJs: Boolean = false,
      csp: Option[ContentSecurityPolicy] = None,
      wrapClass: String = "",
      canonicalPath: Option[CanonicalPath] = None,
      withHrefLangs: Option[lila.i18n.LangList.AlternativeLangs] = None
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
            if (ctx.blind) "lishogi"
            else if (isProd) fullTitle | s"$title | lishogi.org"
            else s"[dev] ${fullTitle | s"$title | lishogi.dev"}"
          },
          cssTag("site"),
          ctx.pageData.inquiry.isDefined option cssTagNoTheme("mod.inquiry"),
          ctx.userContext.impersonatedBy.isDefined option cssTagNoTheme("mod.impersonate"),
          ctx.blind option cssTagNoTheme("blind"),
          moreCss,
          pieceSprite,
          meta(
            content := openGraph.fold(trans.siteDescription.txt())(o => o.description),
            name    := "description"
          ),
          link(rel := "mask-icon", href := staticUrl("logo/lishogi.svg"), attr("color") := "black"),
          favicons,
          !robots option raw("""<meta content="noindex, nofollow" name="robots">"""),
          noTranslate,
          openGraph.map(_.frags(ctx.lang)),
          link(
            href     := routes.Blog.atom,
            `type`   := "application/atom+xml",
            rel      := "alternate",
            st.title := trans.blog.txt()
          ),
          fontPreload,
          boardPreload,
          manifests,
          jsLicense,
          canonicalPath.ifTrue(robots).map(canonical),
          withHrefLangs
            .ifTrue {
              robots &&
              ctx.req.queryString.removed("lang").isEmpty &&
              canonicalPath.fold(true)(_.value == ctx.req.path)
            }
            .map(hrefLangs)
        ),
        st.body(
          cls := List(
            s"${ctx.currentBg} ${ctx.currentTheme.cssClass} coords-${ctx.pref.coordsClass}" -> true,
            s"grid-width-${ctx.pref.customThemeOrDefault.gridWidth}" -> ctx.pref.isUsingCustomTheme,
            "thick-grid"                                             -> ctx.pref.isUsingThickGrid,
            "clear-hands"                                            -> ctx.pref.clearHands,
            "hands-background"                                       -> ctx.pref.handsBackground,
            "no-touch"                                               -> !ctx.pref.squareOverlay,
            "zen"                                                    -> ctx.pref.isZen,
            "blind-mode"                                             -> ctx.blind,
            "kid"                                                    -> ctx.kid,
            "mobile"                                                 -> ctx.isMobileBrowser,
            "playing"                                                -> playing
          ),
          dataDev           := (!isProd).option("true"),
          dataVapid         := vapidPublicKey,
          dataUser          := ctx.userId,
          dataDate          := (ctx.req.path == "/").option(showEnglishDayMonth(DateTime.now)),
          dataSoundSet      := ctx.currentSoundSet.toString,
          dataSocketDomains := socketDomains.mkString(","),
          dataAssetUrl      := assetBaseUrl,
          dataAssetVersion  := assetVersion.value,
          dataNonce         := ctx.nonce.ifTrue(sameAssetDomain).map(_.value),
          dataTheme         := ctx.currentBg,
          dataPieceSet      := ctx.currentPieceSet.key,
          dataChuPieceSet   := ctx.currentChuPieceSet.key,
          dataKyoPieceSet   := ctx.currentKyoPieceSet.key,
          dataAnnounce      := AnnounceStore.get.map(a => safeJsonValue(a.json)),
          dataNotation      := ctx.pref.notation.toString,
          dataColorName     := ctx.pref.colorName.toString,
          style             := cssVariables(zoomable)
        )(
          blindModeForm,
          ctx.pageData.inquiry map { views.html.mod.inquiry(_) },
          ctx.me ifTrue ctx.userContext.impersonatedBy.isDefined map { views.html.mod.impersonate(_) },
          isStage option views.html.base.bits.stage,
          lila.security.EmailConfirm.cookie.get(ctx.req).map(views.html.auth.bits.checkYourEmailBanner(_)),
          playing option zenToggle,
          siteHeader(playing),
          div(
            id := "main-wrap",
            cls := List(
              wrapClass -> wrapClass.nonEmpty
            )
          )(body),
          ctx.isAuth option div(
            id          := "friend_box",
            dataPreload := safeJsonValue(Json.obj("i18n" -> i18nJsObject(i18nKeys)))
          )(
            div(cls := "friend_box_title")(trans.nbFriendsOnline.plural(0, iconTag("S"))),
            div(cls   := "content_wrap none")(
              div(cls := "content list")
            )
          ),
          a(id := "reconnecting", cls := "link text", dataIcon := "B")(trans.reconnecting()),
          shogiground option jsTag("vendor/shogiground.min.js"),
          ctx.requiresFingerprint option fingerprintTag,
          if (minifiedAssets)
            jsModule("site", defer = deferJs)
          else
            frag(
              jsModule("deps", defer = deferJs),
              jsModule("site", defer = deferJs)
            ),
          moreJs,
          embedJsUnsafe(s"""lishogi.quantity=${lila.i18n.JsQuantity(ctx.lang)};$timeagoLocaleScript"""),
          ctx.pageData.inquiry.isDefined option jsTag("inquiry.js", defer = deferJs)
        )
      )
    )

  object siteHeader {

    private val topnavToggle = spaceless(
      """
<input type="checkbox" id="tn-tg" class="topnav-toggle fullscreen-toggle" aria-label="Navigation">
<label for="tn-tg" class="fullscreen-mask"></label>
<label for="tn-tg" class="hbg"><span class="hbg__in"></span></label>"""
    )

    private def reports(implicit ctx: Context) =
      isGranted(_.SeeReport) option
        a(
          cls       := "link data-count link-center",
          title     := "Moderation",
          href      := routes.Report.list,
          dataCount := blockingReportNbOpen,
          dataIcon  := ""
        )

    private def teamRequests(implicit ctx: Context) =
      ctx.teamNbRequests > 0 option
        a(
          cls       := "link data-count link-center",
          href      := routes.Team.requests,
          dataCount := ctx.teamNbRequests,
          dataIcon  := "f",
          title     := trans.team.teams.txt()
        )

    // For Japanese let's force lang param - to build backlinks for SEO - for now
    // unless JP comes from param itself, langHref will take care of that
    private def siteUrl(implicit ctx: Context) =
      if (ctx.lang.language == "ja" && ctx.req.getQueryString("lang").isEmpty)
        urlWithLangQuery("/", "ja")
      else langHref("/")

    def apply(playing: Boolean)(implicit ctx: Context) =
      header(id := "top")(
        div(cls := "site-title-nav")(
          topnavToggle,
          (if (ctx.req.path == "/") h1 else div) (cls := "site-title")(
            if (ctx.kid) span(title := trans.kidMode.txt(), cls := "kiddo")(":)")
            else ctx.isBot option botImage,
            a(href := siteUrl)(
              "lishogi",
              span(if (isProd) ".org" else ".dev"),
              span(cls := "site-beta")("beta")
            )
          ),
          ctx.blind option h2("Navigation"),
          topnav()
        ),
        div(cls := "site-buttons")(
          (ctx.isAnon && ctx.req.path == "/") option switchLanguage,
          clinput,
          reports,
          teamRequests,
          ctx.me map { me =>
            frag(allNotifications, dasher(me))
          } getOrElse { !ctx.pageData.error option anonDasher(playing) }
        )
      )
  }

  private val i18nKeys = List(trans.nbFriendsOnline.key)
}
