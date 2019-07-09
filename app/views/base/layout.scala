package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ Lang, ContentSecurityPolicy }
import lila.pref.Pref

import controllers.routes

object layout {

  object bits {
    val doctype = raw("<!doctype html>")
    def htmlTag(implicit lang: Lang) = html(st.lang := lang.language)
    val topComment = raw("""<!-- Lichess is open source! See https://github.com/ornicar/lila -->""")
    val charset = raw("""<meta charset="utf-8">""")
    val viewport = raw("""<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"/>""")
    def metaCsp(csp: ContentSecurityPolicy): Option[Frag] =
      cspEnabled() option raw(
        s"""<meta http-equiv="Content-Security-Policy" content="$csp">"""
      )
    def metaCsp(csp: Option[ContentSecurityPolicy])(implicit ctx: Context): Option[Frag] =
      metaCsp(csp.getOrElse(defaultCsp))
    def pieceSprite(implicit ctx: Context): Frag = pieceSprite(ctx.currentPieceSet)
    def pieceSprite(ps: lila.pref.PieceSet): Frag =
      link(id := "piece-sprite", href := assetUrl(s"piece-css/$ps.css"), tpe := "text/css", rel := "stylesheet")
  }
  import bits._

  private val noTranslate = raw("""<meta name="google" content="notranslate" />""")
  private def fontPreload(implicit ctx: Context) = raw {
    s"""<link rel="preload" href="${assetUrl(s"font/lichess.woff2")}" as="font" type="font/woff2" crossorigin/>""" +
      !ctx.pref.pieceNotationIsLetter ??
      s"""<link rel="preload" href="${assetUrl(s"font/lichess.chess.woff2")}" as="font" type="font/woff2" crossorigin/>"""
  }
  private val manifests = raw("""<link rel="manifest" href="/manifest.json" /><meta name="twitter:site" content="@lichess" />""")

  private val jsLicense = raw("""<link rel="jslicense" href="/source"/>""")

  private val favicons = raw {
    List(256, 128, 64) map { px =>
      s"""<link rel="icon" type="image/png" href="${staticUrl(s"favicon.$px.png")}" sizes="${px}x${px}"/>"""
    } mkString
  }
  private def blindModeForm(implicit ctx: Context) = raw(s"""<form id="blind-mode" action="${routes.Main.toggleBlindMode}" method="POST"><input type="hidden" name="enable" value="${if (ctx.blind) 0 else 1}" /><input type="hidden" name="redirect" value="${ctx.req.path}" /><button type="submit">Accessibility: ${if (ctx.blind) "Disable" else "Enable"} blind mode</button></form>""")
  private val zenToggle = raw("""<a data-icon="E" id="zentog" class="text fbt active">ZEN MODE</a>""")
  private def dasher(me: lila.user.User) = raw(s"""<div class="dasher"><a id="user_tag" class="toggle link">${me.username}</a><div id="dasher_app" class="dropdown"></div></div>""")

  private def allNotifications(implicit ctx: Context) = spaceless(s"""<div>
  <a id="challenge-toggle" class="toggle link">
    <span title="${trans.challenges().render}" class="data-count" data-count="${ctx.nbChallenges}" data-icon="U"></span>
  </a>
  <div id="challenge-app" class="dropdown"></div>
</div>
<div>
  <a id="notify-toggle" class="toggle link">
    <span title="${trans.notifications().render}" class="data-count" data-count="${ctx.nbNotifications}" data-icon=""></span>
  </a>
  <div id="notify-app" class="dropdown"></div>
</div>""")

  private def anonDasher(playing: Boolean)(implicit ctx: Context) = spaceless(s"""<div class="dasher">
  <a class="toggle link anon">
    <span title="${trans.preferences().render}" data-icon="%"</span>
  </a>
  <div id="dasher_app" class="dropdown" data-playing="$playing"></div>
</div>
<a href="${routes.Auth.login}?referrer=${ctx.req.path}" class="signin button button-empty">${trans.signIn().render}</a>""")

  private val clinputLink = a(cls := "link")(span(dataIcon := "y"))

  private def clinput(implicit ctx: Context) =
    div(id := "clinput")(
      clinputLink,
      input(spellcheck := "false", placeholder := trans.search.txt())
    )

  private lazy val botImage = img(src := staticUrl("images/icons/bot.png"), title := "Robot chess", style :=
    "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;vertical-align:text-top")

  private val spaceRegex = """\s{2,}+""".r
  private def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  private val dataVapid = attr("data-vapid")
  private val dataUser = attr("data-user")
  private val dataSoundSet = attr("data-sound-set")
  private val dataSocketDomain = attr("data-socket-domain")
  private val dataRemoteSocketDomain = attr("data-remote-socket-domain")
  private val dataZoom = attr("data-zoom")
  private val dataPreload = attr("data-preload")
  private val dataPlaying = attr("data-playing")
  private val dataPatrons = attr("data-patrons")
  private val dataStudying = attr("data-studying")
  private val dataNonce = attr("data-nonce")

  def apply(
    title: String,
    fullTitle: Option[String] = None,
    robots: Boolean = isGloballyCrawlable,
    moreCss: Frag = emptyFrag,
    moreJs: Frag = emptyFrag,
    playing: Boolean = false,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    chessground: Boolean = true,
    zoomable: Boolean = false,
    deferJs: Boolean = false,
    csp: Option[ContentSecurityPolicy] = None,
    wrapClass: String = ""
  )(body: Frag)(implicit ctx: Context) = frag(
    doctype,
    htmlTag(ctx.lang)(
      topComment,
      head(
        charset,
        viewport,
        metaCsp(csp),
        if (isProd && !isStage) frag(
          st.headTitle(fullTitle | s"$title • lichess.org")
        )
        else st.headTitle(s"[dev] ${fullTitle | s"$title • lichess.dev"}"),
        cssTag("site"),
        ctx.pref.is3d option cssTag("board-3d"),
        ctx.pageData.inquiry.isDefined option cssTagNoTheme("mod.inquiry"),
        ctx.userContext.impersonatedBy.isDefined option cssTagNoTheme("mod.impersonate"),
        ctx.blind option cssTagNoTheme("blind"),
        moreCss,
        pieceSprite,
        meta(content := openGraph.fold(trans.siteDescription.txt())(o => o.description), name := "description"),
        link(id := "favicon", rel := "shortcut icon", href := staticUrl("images/favicon-32-white.png"), `type` := "image/x-icon"),
        link(rel := "mask-icon", href := staticUrl("favicon.svg"), color := "black"),
        favicons,
        !robots option raw("""<meta content="noindex, nofollow" name="robots">"""),
        noTranslate,
        openGraph.map(_.frags),
        link(href := routes.Blog.atom, `type` := "application/atom+xml", rel := "alternate", st.title := trans.blog.txt()),
        ctx.transpBgImg map { img =>
          raw(s"""<style type="text/css" id="bg-data">body.transp::before{background-image:url('$img');}</style>""")
        },
        fontPreload,
        manifests,
        jsLicense
      ),
      st.body(
        cls := List(
          s"${ctx.currentBg} ${ctx.currentTheme.cssClass} ${ctx.currentTheme3d.cssClass} ${ctx.currentPieceSet3d.toString} coords-${ctx.pref.coordsClass}" -> true,
          "piece-letter" -> ctx.pref.pieceNotationIsLetter,
          "zen" -> ctx.pref.isZen,
          "blind-mode" -> ctx.blind,
          "kid" -> ctx.kid,
          "mobile" -> ctx.isMobileBrowser,
          "playing fixed-scroll" -> playing,
          "coords-out" -> (ctx.pref.coords == Pref.Coords.OUTSIDE)
        ),
        dataDev := (!isProd).option("true"),
        dataVapid := usesServiceWorker option vapidPublicKey,
        dataUser := ctx.userId,
        dataSoundSet := ctx.currentSoundSet.toString,
        dataSocketDomain := socketDomain,
        dataRemoteSocketDomain := maybeRemoteSocketDomain,
        dataAssetUrl := assetBaseUrl,
        dataAssetVersion := assetVersion.value,
        dataNonce := ctx.nonce.ifTrue(sameAssetDomain).map(_.value),
        dataTheme := ctx.currentBg,
        style := zoomable option s"--zoom:${ctx.zoom}"
      )(
          blindModeForm,
          ctx.pageData.inquiry map { views.html.mod.inquiry(_) },
          ctx.me ifTrue ctx.userContext.impersonatedBy.isDefined map { views.html.mod.impersonate(_) },
          isStage option views.html.base.bits.stage,
          lila.security.EmailConfirm.cookie.get(ctx.req).map(views.html.auth.bits.checkYourEmailBanner(_)),
          playing option zenToggle,
          siteHeader(playing),
          div(id := "main-wrap", cls := List(
            wrapClass -> wrapClass.nonEmpty,
            "is2d" -> ctx.pref.is2d,
            "is3d" -> ctx.pref.is3d
          ))(body),
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
                  trans.onlineFriends()
                ),
                div(cls := "content_wrap")(
                  div(cls := "content list"),
                  div(cls := List(
                    "nobody" -> true,
                    "none" -> ctx.onlineFriends.users.nonEmpty
                  ))(
                    span(trans.noFriendsOnline()),
                    a(cls := "find button", href := routes.User.opponents)(
                      span(cls := "is3 text", dataIcon := "h")(trans.findFriends())
                    )
                  )
                )
              )
          },
          a(id := "reconnecting", cls := "link text", dataIcon := "B")(trans.reconnecting()),
          chessground option jsTag("vendor/chessground.min.js"),
          ctx.requiresFingerprint option fingerprintTag,
          if (isProd)
            jsAt(s"compiled/lichess.site.min.js", defer = deferJs)
          else frag(
            jsAt(s"compiled/lichess.deps.js", defer = deferJs),
            jsAt(s"compiled/lichess.site.js", defer = deferJs)
          ),
          moreJs,
          embedJsUnsafe(s"""lichess.quantity=${lila.i18n.JsQuantity(ctx.lang)};$timeagoLocaleScript"""),
          ctx.pageData.inquiry.isDefined option jsTag("inquiry.js", defer = deferJs)
        )
    )
  )

  object siteHeader {

    private val topnavToggle = spaceless("""
<input type="checkbox" id="tn-tg" class="topnav-toggle fullscreen-toggle" aria-label="Navigation">
<label for="tn-tg" class="fullscreen-mask"></label>
<label for="tn-tg" class="hbg"><span class="hbg__in"></span></label>""")

    private def reports(implicit ctx: Context) = isGranted(_.SeeReport) option
      a(cls := "link data-count link-center", title := "Moderation", href := routes.Report.list, dataCount := reportNbOpen, dataIcon := "")

    private def teamRequests(implicit ctx: Context) = ctx.teamNbRequests > 0 option
      a(cls := "link data-count link-center", href := routes.Team.requests, dataCount := ctx.teamNbRequests, dataIcon := "f", title := trans.teams.txt())

    def apply(playing: Boolean)(implicit ctx: Context) =
      header(id := "top")(
        div(cls := "site-title-nav")(
          topnavToggle,
          h1(cls := "site-title")(
            if (ctx.kid) span(title := trans.kidMode.txt(), cls := "kiddo")(":)")
            else ctx.isBot option botImage,
            a(href := "/")(
              "lichess",
              span(if (isProd && !isStage) ".org" else ".dev")
            )
          ),
          ctx.blind option h2("Navigation"),
          topnav()
        ),
        div(cls := "site-buttons")(
          clinput,
          reports,
          teamRequests,
          ctx.me map { me =>
            frag(allNotifications, dasher(me))
          } getOrElse { !ctx.pageData.error option anonDasher(playing) }
        )
      )
  }
}
