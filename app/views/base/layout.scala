package views.html.base

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.ContentSecurityPolicy

import controllers.routes

object layout {

  object bits {
    val doctype = raw("<!doctype html>")
    def htmlTag(implicit ctx: Context) = html(st.lang := ctx.lang.language)
    val topComment = raw("""<!-- Lichess is open source! See https://github.com/ornicar/lila -->""")
    val charset = raw("""<meta charset="utf-8">""")
    val viewport = raw("""<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"/>""")
    def metaCsp(csp: Option[ContentSecurityPolicy])(implicit ctx: Context): Option[Frag] =
      cspEnabled() option raw(
        s"""<meta http-equiv="Content-Security-Policy" content="${csp.getOrElse(defaultCsp)}">"""
      )
    def currentBgCss(implicit ctx: Context) = ctx.currentBg match {
      case "dark" => cssTag("dark.css")
      case "transp" => cssTags("dark.css", "transp.css")
      case _ => emptyHtml
    }
    def pieceSprite(implicit ctx: Context) =
      link(id := "piece-sprite", href := assetUrl(s"stylesheets/piece/${ctx.currentPieceSet}.css"), `type` := "text/css", rel := "stylesheet")
    val fontStylesheets = raw(List(
      """<link href="https://fonts.googleapis.com/css?family=Noto+Sans:400,700|Roboto:300" rel="stylesheet">""",
      """<link href="https://fonts.googleapis.com/css?family=Roboto+Mono:500&text=0123456789:." rel="stylesheet">"""
    ).mkString)
  }
  import bits._

  private val noTranslate = raw("""<meta name="google" content="notranslate" />""")
  private val fontPreload = raw(s"""<link rel="preload" href="${assetUrl(s"font/lichess/fonts/lichess.woff")}" as="font" type="font/woff" crossorigin/>""")
  private val manifests = raw(List(
    """<link rel="manifest" href="/manifest.json" />""",
    """<meta name="twitter:site" content="@lichess" />"""
  ).mkString)

  private val favicons = raw {
    List(256, 128, 64) map { px =>
      s"""<link rel="icon" type="image/png" href="${staticUrl(s"favicon.$px.png")}" sizes="${px}x${px}"/>"""
    } mkString
  }
  private def blindModeForm(implicit ctx: Context) = raw(s"""<form id="blind_mode" action="${routes.Main.toggleBlindMode}" method="POST"><input type="hidden" name="enable" value="${if (ctx.blind) 0 else 1}" /><input type="hidden" name="redirect" value="${ctx.req.path}" /><button type="submit">Accessibility: ${if (ctx.blind) "Disable" else "Enable"} blind mode</button></form>""")
  private val zenToggle = raw("""<a data-icon="E" id="zentog" class="text fbt active">ZEN MODE</a>""")
  private def dasher(me: lila.user.User) = raw(s"""<div class="dasher"><a id="user_tag" class="toggle link">${me.username}</a><div id="dasher_app" class="dropdown"></div></div>""")

  private def allNotifications(implicit ctx: Context) = spaceless(s"""<div class="challenge_notifications">
  <a id="challenge_notifications_tag" class="toggle link">
    <span title="${trans.challenges.txt()}" class="data-count" data-count="${ctx.nbChallenges}" data-icon="U"></span>
  </a>
  <div id="challenge_app" class="dropdown"></div>
</div>
<div class="site_notifications">
  <a id="site_notifications_tag" class="toggle link">
    <span title="${trans.notifications.txt()}" class="data-count" data-count="${ctx.nbNotifications}" data-icon=""</span>
  </a>
  <div id="notify_app" class="dropdown"></div>
</div>""")

  private def anonDasher(playing: Boolean)(implicit ctx: Context) = spaceless(s"""<div class="dasher">
  <a class="toggle link anon">
    <span title="${trans.preferences.txt()}" data-icon="%"</span>
  </a>
  <div id="dasher_app" class="dropdown" data-playing="$playing"></div>
</div>
<a href="${routes.Auth.login}?referrer=${currentPath}" class="signin button">${trans.signIn.txt()}</a>""")

  private val clinputLink = a(cls := "link")(span(dataIcon := "y"))

  private def clinput(implicit ctx: Context) =
    div(id := "clinput")(
      clinputLink,
      input(spellcheck := "false", placeholder := trans.search.txt())
    )

  private lazy val botImage = img(src := staticUrl("images/icons/bot.png"), title := "Robot chess", style := "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;")

  private val spaceRegex = """\s{2,}+""".r
  private def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  private val dataDev = attr("data-dev")
  private val dataUser = attr("data-user")
  private val dataSoundSet = attr("data-sound-set")
  private val dataSocketDomain = attr("data-socket-domain")
  private val dataAssetUrl = attr("data-asset-url")
  private val dataAssetVersion = attr("data-asset-version")
  private val dataNonce = attr("data-nonce")
  private val dataZoom = attr("data-zoom")
  private val dataTheme = attr("data-theme")
  private val dataResp = attr("data-resp")
  private val dataPreload = attr("data-preload")
  private val dataPlaying = attr("data-playing")
  private val dataPatrons = attr("data-patrons")
  private val dataStudying = attr("data-studying")

  def apply(
    title: String,
    fullTitle: Option[String] = None,
    side: Option[Html] = None,
    menu: Option[Html] = None,
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    robots: Boolean = isGloballyCrawlable,
    moreCss: Html = emptyHtml,
    moreJs: Html = emptyHtml,
    playing: Boolean = false,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    chessground: Boolean = true,
    zoomable: Boolean = false,
    asyncJs: Boolean = false,
    csp: Option[ContentSecurityPolicy] = None,
    responsive: Boolean = false,
    wrapClass: String = ""
  )(body: Html)(implicit ctx: Context) = frag(
    doctype,
    htmlTag(ctx)(
      topComment,
      head(
        charset,
        responsive option viewport,
        metaCsp(csp),
        if (isProd) frag(
          st.headTitle(fullTitle | s"$title • lichess.org"),
          !responsive option fontStylesheets
        )
        else st.headTitle(s"[dev] ${fullTitle | s"$title • lichess.org"}"),
        if (responsive) frag(
          responsiveCssTag("site"),
          ctx.pref.is3d option responsiveCssTag("board-3d")
        )
        else frag(
          responsive option cssTag("offline-fonts.css"),
          currentBgCss,
          cssTag("common.css"),
          cssTag("board.css"),
          ctx.pref.is3d option cssTag("board-3d.css"),
          ctx.zoom ifTrue zoomable map { z =>
            zoomStyle(z / 100f, ctx.pref.is3d)
          }
        ),
        ctx.pref.coords == 1 option cssTag("board.coords.inner.css"),
        ctx.pageData.inquiry.isDefined option cssTag("inquiry.css"),
        ctx.userContext.impersonatedBy.isDefined option cssTag("impersonate.css"),
        isStage option responsiveCssTagNoTheme("stage"),
        moreCss,
        pieceSprite,
        meta(content := openGraph.fold(trans.siteDescription.txt())(o => o.description), name := "description"),
        link(id := "favicon", rel := "shortcut icon", href := staticUrl("images/favicon-32-white.png"), `type` := "image/x-icon"),
        link(rel := "mask-icon", href := staticUrl("favicon.svg"), color := "black"),
        favicons,
        !robots option raw("""<meta content="noindex, nofollow" name="robots">"""),
        noTranslate,
        openGraph.map(_.frag),
        link(href := routes.Blog.atom, `type` := "application/atom+xml", rel := "alternate", st.title := trans.blog.txt()),
        ctx.transpBgImg map { img =>
          raw(s"""<style type="text/css" id="bg-data">body.transp::before{background-image:url('$img');}</style>""")
        },
        fontPreload,
        manifests
      ),
      st.body(
        cls := List(
          "preload base" -> true,
          ctx.currentTheme.cssClass -> true,
          ctx.currentTheme3d.cssClass -> true,
          (if (ctx.currentBg == "transp") "dark transp" else ctx.currentBg) -> true,
          ctx.currentPieceSet3d.toString -> true,
          "piece_letter" -> ctx.pref.pieceNotationIsLetter,
          "zen" -> ctx.pref.isZen,
          "blind_mode" -> ctx.blind,
          "kid" -> ctx.kid,
          "mobile" -> ctx.isMobileBrowser,
          "playing fixed-scroll" -> playing
        ),
        dataDev := (!isProd).option("true"),
        dataUser := ctx.userId,
        dataSoundSet := ctx.currentSoundSet.toString,
        dataSocketDomain := socketDomain,
        dataAssetUrl := assetBaseUrl,
        dataAssetVersion := assetVersion.value,
        dataNonce := ctx.nonce.map(_.value),
        dataZoom := ctx.zoom.ifFalse(responsive).map(_.toString),
        dataResp := responsive.option(true),
        dataTheme := responsive.option(ctx.currentBg),
        style := zoomable option s"--zoom:${ctx.respZoom}"
      )(
          blindModeForm,
          ctx.pageData.inquiry map { views.html.mod.inquiry(_) },
          ctx.me ifTrue ctx.userContext.impersonatedBy.isDefined map { views.html.mod.impersonate(_) },
          isStage option div(id := "stage")(
            "This is an empty lichess preview website for developers. ",
            a(href := "https://lichess.org")("Go to lichess.org instead")
          ),
          lila.security.EmailConfirm.cookie.get(ctx.req).map(views.html.auth.emailConfirmBanner(_)),
          playing option zenToggle,
          if (responsive) siteHeader.responsive(playing)
          else siteHeader.old(playing),
          if (responsive) div(id := "main-wrap", cls := List(
            wrapClass -> wrapClass.nonEmpty,
            "is2d" -> ctx.pref.is2d,
            "is3d" -> ctx.pref.is3d
          ))(body)
          else div(cls := s"content ${if (ctx.pref.is3d) "is3d" else "is2d"}")(
            div(id := "site_header")(
              div(id := "notifications"),
              div(cls := "board_left")(
                h1(
                  a(id := "site_title", href := routes.Lobby.home)(
                    if (ctx.kid) span(st.title := trans.kidMode.txt(), cls := "kiddo")("😊")
                    else ctx.isBot option botImage,
                    "lichess",
                    span(cls := "extension")(if (isProd) ".org" else " dev")
                  )
                ),
                menu map { sideMenu =>
                  div(cls := "side_menu")(sideMenu)
                },
                side,
                chat
              ),
              underchat map { g =>
                div(cls := "under_chat")(g)
              }
            ),
            div(id := "lichess")(body)
          ),
          ctx.me.map { me =>
            div(
              id := "friend_box",
              dataPreload := ctx.onlineFriends.users.map(_.titleName).mkString(","),
              dataPlaying := ctx.onlineFriends.playing.mkString(","),
              dataPatrons := ctx.onlineFriends.patrons.mkString(","),
              dataStudying := ctx.onlineFriends.studying.mkString(",")
            )(
                div(cls := "friend_box_title")(
                  strong(cls := "online")("?"),
                  " ",
                  trans.onlineFriends.frag()
                ),
                div(cls := "content_wrap")(
                  div(cls := "content list"),
                  div(cls := List(
                    "nobody" -> true,
                    "none" -> ctx.onlineFriends.users.nonEmpty
                  ))(
                    span(trans.noFriendsOnline.frag()),
                    a(cls := "find button", href := routes.User.opponents)(
                      span(cls := "is3 text", dataIcon := "h")(trans.findFriends.frag())
                    )
                  )
                )
              )
          },
          chessground option jsTag("vendor/chessground.min.js"),
          ctx.requiresFingerprint option fingerprintTag,
          jsAt(s"compiled/lichess.site${isProd ?? ".min"}.js", async = asyncJs),
          moreJs,
          embedJs(s"""lichess.quantity=${lila.i18n.JsQuantity(ctx.lang)};$timeagoLocaleScript;"""),
          ctx.pageData.inquiry.isDefined option jsTag("inquiry.js", async = asyncJs)
        )
    )
  )

  object siteHeader {

    private val topnavToggle = spaceless("""
<input type="checkbox" id="tn-tg" class="topnav-toggle fullscreen-toggle" aria-label="Navigation">
<label for="tn-tg" class="fullscreen-mask"></label>
<label for="tn-tg" class="hbg"><span class="hbg__in"></span></label>""")

    private def reconnecting(implicit ctx: Context) =
      a(id := "reconnecting", cls := "link text", dataIcon := "B")(trans.reconnecting.frag())

    private def reports(implicit ctx: Context) = isGranted(_.SeeReport) option
      a(cls := "link data-count", title := "Moderation", href := routes.Report.list, dataCount := reportNbOpen, dataIcon := "")

    private def teamRequests(implicit ctx: Context) = ctx.teamNbRequests > 0 option
      a(cls := "link data-count", href := routes.Team.requests, dataCount := ctx.teamNbRequests, dataIcon := "f", title := trans.teams.txt())

    def responsive(playing: Boolean)(implicit ctx: Context) =
      header(id := "top")(
        div(cls := "site-title-nav")(
          topnavToggle,
          h1(cls := "site-title")(
            a(href := "/")(
              if (ctx.kid) span(title := trans.kidMode.txt(), cls := "kiddo")("😊")
              else ctx.isBot option botImage,
              "lichess",
              span(if (isProd) ".org" else " dev")
            )
          ),
          topmenu()
        ),
        reconnecting,
        div(cls := "site-buttons")(
          clinput,
          reports,
          teamRequests,
          ctx.me map { me =>
            frag(allNotifications, dasher(me))
          } getOrElse { !ctx.pageData.error option anonDasher(playing) }
        )
      )

    def old(playing: Boolean)(implicit ctx: Context) =
      div(id := "top", cls := (if (ctx.pref.is3d) "is3d" else "is2d"))(
        topmenu(),
        div(id := "ham-plate", cls := "link", title := trans.menu.txt())(
          div(id := "hamburger", dataIcon := "[")
        ),
        ctx.me map { me =>
          frag(dasher(me), allNotifications)
        } getOrElse {
          !ctx.pageData.error option anonDasher(playing)
        },
        teamRequests,
        reports,
        clinput,
        reconnecting
      )
  }
}
