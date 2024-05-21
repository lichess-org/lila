package lila.web
package ui

import play.api.i18n.Lang

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.i18n.Language
import lila.core.report.ScoreThresholds

final class layout(helpers: Helpers, assetHelper: lila.web.ui.AssetFullHelper)(
    jsQuantity: Lang => String,
    isRTL: Lang => Boolean,
    popularAlternateLanguages: List[Language],
    reportScoreThreshold: () => ScoreThresholds,
    reportScore: () => Int
):
  import helpers.{ *, given }
  import assetHelper.{ defaultCsp, netConfig, cashTag, jsName, siteName }

  val doctype                                 = raw("<!DOCTYPE html>")
  def htmlTag(using lang: Lang, ctx: Context) = html(st.lang := lang.code, dir := isRTL(lang).option("rtl"))
  val topComment = raw("""<!-- Lichess is open source! See https://lichess.org/source -->""")
  val charset    = raw("""<meta charset="utf-8">""")
  val viewport = raw:
    """<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">"""
  def metaCsp(csp: ContentSecurityPolicy): Frag = raw:
    s"""<meta http-equiv="Content-Security-Policy" content="${lila.web.ContentSecurityPolicy.render(csp)}">"""
  def metaCsp(csp: Option[ContentSecurityPolicy])(using Context, Option[Nonce]): Frag =
    metaCsp(csp.getOrElse(defaultCsp))
  def systemThemeScript(nonce: Option[Nonce]) =
    embedJsUnsafe(
      "if (window.matchMedia('(prefers-color-scheme: light)')?.matches) " +
        "document.documentElement.classList.add('light');"
    )(nonce)
  def pieceSprite(name: String): Frag =
    link(id := "piece-sprite", href := assetUrl(s"piece-css/$name.css"), rel := "stylesheet")
  val noTranslate = raw("""<meta name="google" content="notranslate">""")

  def preload(href: String, as: String, crossorigin: Boolean, tpe: Option[String] = None) =
    val linkType = tpe.so(t => s"""type="$t" """)
    raw:
      s"""<link rel="preload" href="$href" as="$as" $linkType${crossorigin.so("crossorigin")}>"""

  def fontPreload(using ctx: Context) = frag(
    preload(assetUrl("font/lichess.woff2"), "font", crossorigin = true, "font/woff2".some),
    preload(
      staticAssetUrl("font/noto-sans-v14-latin-regular.woff2"),
      "font",
      crossorigin = true,
      "font/woff2".some
    ),
    (!ctx.pref.pieceNotationIsLetter).option(
      preload(staticAssetUrl("font/lichess.chess.woff2"), "font", crossorigin = true, "font/woff2".some)
    )
  )

  def allNotifications(challenges: Int, notifs: Int)(using Translate) =
    val challengeTitle = trans.challenge.challengesX.txt(challenges)
    val notifTitle     = trans.site.notificationsX.txt(notifs)
    spaceless:
      s"""<div>
  <button id="challenge-toggle" class="toggle link">
    <span title="$challengeTitle" role="status" aria-label="$challengeTitle" class="data-count" data-count="$challenges" data-icon="${Icon.Swords}"></span>
  </button>
  <div id="challenge-app" class="dropdown"></div>
</div>
<div>
  <button id="notify-toggle" class="toggle link">
    <span title="$notifTitle" role="status" aria-label="$notifTitle" class="data-count" data-count="$notifs" data-icon="${Icon.BellOutline}"></span>
  </button>
  <div id="notify-app" class="dropdown"></div>
</div>"""

  def clinput(using ctx: Context) =
    val label = trans.search.search.txt()
    div(id := "clinput")(
      a(cls := "link")(span(dataIcon := Icon.Search)),
      input(
        spellcheck   := "false",
        autocomplete := ctx.blind.toString,
        aria.label   := label,
        placeholder  := label,
        enterkeyhint := "search"
      )
    )

  val warnNoAutoplay =
    div(id := "warn-no-autoplay")(
      a(dataIcon := Icon.Mute, target := "_blank", href := s"${routes.Main.faq}#autoplay")
    )

  def botImage =
    img(
      src   := staticAssetUrl("images/icons/bot.png"),
      title := "Robot chess",
      style := "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;vertical-align:text-top"
    )

  val manifests = raw:
    """<link rel="manifest" href="/manifest.json"><meta name="twitter:site" content="@lichess">"""

  val favicons = raw:
    List(512, 256, 192, 128, 64)
      .map: px =>
        s"""<link rel="icon" type="image/png" href="$assetBaseUrl/assets/logo/lichess-favicon-$px.png" sizes="${px}x$px">"""
      .mkString(
        "",
        "",
        s"""<link id="favicon" rel="icon" type="image/png" href="$assetBaseUrl/assets/logo/lichess-favicon-32.png" sizes="32x32">"""
      )
  def blindModeForm(using ctx: Context) = raw:
    s"""<form id="blind-mode" action="${routes.Main.toggleBlindMode}" method="POST"><input type="hidden" name="enable" value="${
        if ctx.blind then 0 else 1
      }"><input type="hidden" name="redirect" value="${ctx.req.path}"><button type="submit">Accessibility: ${
        if ctx.blind then "Disable" else "Enable"
      } blind mode</button></form>"""

  def zenZone(using Translate) = spaceless:
    s"""
<div id="zenzone">
  <a href="/" class="zen-home"></a>
  <a data-icon="${Icon.Checkmark}" id="zentog" class="text fbt active">${trans.preferences.zenMode
        .txt()}</a>
</div>"""

  def dasher(me: User) =
    div(cls := "dasher")(
      a(id := "user_tag", cls := "toggle link", href := routes.Auth.logoutGet)(me.username),
      div(id := "dasher_app", cls := "dropdown")
    )

  def anonDasher(using ctx: Context) =
    val prefs = trans.preferences.preferences.txt()
    frag(
      a(href := s"${routes.Auth.login.url}?referrer=${ctx.req.path}", cls := "signin")(trans.site.signIn()),
      div(cls := "dasher")(
        button(cls := "toggle anon link", title := prefs, aria.label := prefs, dataIcon := Icon.Gear),
        div(id     := "dasher_app", cls         := "dropdown")
      )
    )

  // consolidate script packaging here to dedup chunk dependencies
  def sitePreload(modules: EsmList, isInquiry: Boolean)(using ctx: Context) =
    scriptsPreload("site" :: (isInquiry.option("mod.inquiry") :: modules.map(_.map(_.key))).flatten)

  def scriptsPreload(keys: List[String]) =
    frag(
      jsTag("manifest"),
      cashTag,
      keys.map(jsTag),
      assetHelper.manifest.deps(keys).map(jsTag)
    )

  private def jsTag(key: String): Frag =
    script(tpe := "module", src := staticAssetUrl(s"compiled/${jsName(key)}"))

  def modulesInit(modules: EsmList)(using ctx: PageContext) =
    modules.flatMap(_.map(_.init(ctx.nonce))) // in body

  private def hrefLang(langStr: String, path: String) =
    s"""<link rel="alternate" hreflang="$langStr" href="$netBaseUrl$path"/>"""

  def hrefLangs(path: LangPath) = raw {
    val pathEnd = if path.value == "/" then "" else path.value
    hrefLang("x-default", path.value) + hrefLang("en", path.value) +
      popularAlternateLanguages.map { l =>
        hrefLang(l.value, s"/$l$pathEnd")
      }.mkString
  }

  def pageZoom(using ctx: Context): Int = {
    def oldZoom = ctx.req.session.get("zoom2").flatMap(_.toIntOption).map(_ - 100)
    ctx.req.cookies
      .get("zoom")
      .map(_.value)
      .flatMap(_.toIntOption)
      .orElse(oldZoom)
      .filter(0 <=)
      .filter(100 >=)
  } | 80

  val dailyNewsAtom = link(
    href     := routes.Feed.atom,
    st.title := "Lichess Updates Feed",
    tpe      := "application/atom+xml",
    rel      := "alternate"
  )

  val dataVapid         = attr("data-vapid")
  val dataSocketDomains = attr("data-socket-domains") := netConfig.socketDomains.mkString(",")
  val dataSocketAlts    = attr("data-socket-alts")    := netConfig.socketAlts.mkString(",")
  val dataNonce         = attr("data-nonce")
  val dataAnnounce      = attr("data-announce")
  val dataSoundSet      = attr("data-sound-set")
  val dataTheme         = attr("data-theme")
  val dataDirection     = attr("data-direction")
  val dataBoard         = attr("data-board")
  val dataPieceSet      = attr("data-piece-set")
  val dataBoard3d       = attr("data-board3d")
  val dataPieceSet3d    = attr("data-piece-set3d")
  val dataAssetUrl      = attr("data-asset-url")      := netConfig.assetBaseUrl.value
  val dataAssetVersion  = attr("data-asset-version")
  val dataDev           = attr("data-dev")            := (!netConfig.minifiedAssets).option("true")

  val spinnerMask = raw:
    """<svg width="0" height="0"><mask id="mask"><path fill="#fff" stroke="#fff" stroke-linejoin="round" d="M38.956.5c-3.53.418-6.452.902-9.286 2.984C5.534 1.786-.692 18.533.68 29.364 3.493 50.214 31.918 55.785 41.329 41.7c-7.444 7.696-19.276 8.752-28.323 3.084C3.959 39.116-.506 27.392 4.683 17.567 9.873 7.742 18.996 4.535 29.03 6.405c2.43-1.418 5.225-3.22 7.655-3.187l-1.694 4.86 12.752 21.37c-.439 5.654-5.459 6.112-5.459 6.112-.574-1.47-1.634-2.942-4.842-6.036-3.207-3.094-17.465-10.177-15.788-16.207-2.001 6.967 10.311 14.152 14.04 17.663 3.73 3.51 5.426 6.04 5.795 6.756 0 0 9.392-2.504 7.838-8.927L37.4 7.171z"/></mask></svg>"""

  val networkAlert = a(id := "network-status", cls := "link text", dataIcon := Icon.ChasingArrows)

  private val spaceRegex      = """\s{2,}+""".r
  def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  val lichessFontFaceCss = spaceless:
    s"""<style>@font-face {
        font-family: 'lichess';
        font-display: block;
        src:
          url('${assetUrl("font/lichess.woff2")}') format('woff2'),
          url('${assetUrl("font/lichess.woff")}') format('woff');
      }</style>"""

  def bottomHtml(using ctx: Context) = frag(
    ctx.me
      .exists(_.enabled.yes)
      .option(
        div(id := "friend_box")(
          div(cls := "friend_box_title")(
            trans.site.nbFriendsOnline.plural(0, iconTag(Icon.UpTriangle))
          ),
          div(cls := "content_wrap none")(
            div(cls := "content list")
          )
        )
      ),
    Option.when(netConfig.socketDomains.nonEmpty)(networkAlert),
    spinnerMask
  )

  object siteHeader:

    private val topnavToggle = spaceless:
      """
<input type="checkbox" id="tn-tg" class="topnav-toggle fullscreen-toggle" autocomplete="off" aria-label="Navigation">
<label for="tn-tg" class="fullscreen-mask"></label>
<label for="tn-tg" class="hbg"><span class="hbg__in"></span></label>"""

    private def reports(using Context) =
      if Granter.opt(_.SeeReport) then
        val threshold = reportScoreThreshold()
        val maxScore  = reportScore()
        a(
          cls := List(
            "link data-count report-maxScore link-center" -> true,
            "report-maxScore--high"                       -> (maxScore > threshold.high),
            "report-maxScore--low"                        -> (maxScore <= threshold.mid)
          ),
          title     := "Moderation",
          href      := routes.Report.list,
          dataCount := maxScore,
          dataIcon  := Icon.Agent
        ).some
      else
        Granter
          .opt(_.PublicChatView)
          .option(
            a(
              cls      := "link",
              title    := "Moderation",
              href     := routes.Mod.publicChat,
              dataIcon := Icon.Agent
            )
          )

    private def teamRequests(nb: Int)(using Translate) =
      Option.when(nb > 0):
        a(
          cls       := "link data-count link-center",
          href      := routes.Team.requests,
          dataCount := nb,
          dataIcon  := Icon.Group,
          title     := trans.team.teams.txt()
        )

    private val siteNameFrag: Frag =
      if siteName == "lichess.org" then frag("lichess", span(".org"))
      else frag(siteName)

    def apply(
        zenable: Boolean,
        isAppealUser: Boolean,
        challenges: Int,
        notifications: Int,
        error: Boolean,
        topnav: Frag
    )(using ctx: PageContext) =
      header(id := "top")(
        div(cls := "site-title-nav")(
          (!isAppealUser).option(topnavToggle),
          a(cls := "site-title", href := langHref("/"))(
            if ctx.kid.yes then span(title := trans.site.kidMode.txt(), cls := "kiddo")(":)")
            else ctx.isBot.option(botImage),
            div(cls := "site-icon", dataIcon := Icon.Logo),
            div(cls := "site-name")(siteNameFrag)
          ),
          (!isAppealUser).option(
            frag(
              topnav,
              (ctx.kid.no && !ctx.me.exists(_.isPatron) && !zenable).option(
                a(cls := "site-title-nav__donate")(
                  href := routes.Plan.index
                )(trans.patron.donate())
              )
            )
          ),
          ctx.blind.option(h2("Navigation"))
        ),
        div(cls := "site-buttons")(
          warnNoAutoplay,
          (!isAppealUser).option(clinput),
          reports,
          teamRequests(ctx.teamNbRequests),
          if isAppealUser then
            postForm(action := routes.Auth.logout):
              submitButton(cls := "button button-red link")(trans.site.logOut())
          else
            ctx.me
              .map: me =>
                frag(allNotifications(challenges, notifications), dasher(me))
              .getOrElse { (!error).option(anonDasher) }
        )
      )

  object inlineJs:
    def apply(nonce: Nonce)(using Translate): Frag = embedJsUnsafe(jsCode)(nonce.some)

    private val i18nKeys = List(
      trans.site.pause,
      trans.site.resume,
      trans.site.nbFriendsOnline,
      trans.site.reconnecting,
      trans.site.noNetwork,
      trans.timeago.justNow,
      trans.timeago.inNbSeconds,
      trans.timeago.inNbMinutes,
      trans.timeago.inNbHours,
      trans.timeago.inNbDays,
      trans.timeago.inNbWeeks,
      trans.timeago.inNbMonths,
      trans.timeago.inNbYears,
      trans.timeago.rightNow,
      trans.timeago.nbMinutesAgo,
      trans.timeago.nbHoursAgo,
      trans.timeago.nbDaysAgo,
      trans.timeago.nbWeeksAgo,
      trans.timeago.nbMonthsAgo,
      trans.timeago.nbYearsAgo,
      trans.timeago.nbMinutesRemaining,
      trans.timeago.nbHoursRemaining,
      trans.timeago.completed
    )

    private val cache = new java.util.concurrent.ConcurrentHashMap[Lang, String]
    lila.common.Bus.subscribeFun("i18n.load"):
      case lang: Lang => cache.remove(lang)

    private def jsCode(using t: Translate) =
      cache.computeIfAbsent(
        t.lang,
        _ =>
          "if (!window.site) window.site={};" +
            """window.site.load=new Promise(r=>document.addEventListener("DOMContentLoaded",r));""" +
            s"window.site.quantity=${jsQuantity(t.lang)};" +
            s"window.site.siteI18n=${safeJsonValue(i18nJsObject(i18nKeys))};"
      )
  end inlineJs
