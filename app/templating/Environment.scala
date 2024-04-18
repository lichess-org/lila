package lila.app
package templating

import lila.ui.ScalatagsTemplate.*
import lila.web.ui.*

import com.softwaremill.macwire.*

object Environment
    extends RouterHelper
    with lila.ui.PaginatorHelper
    with lila.setup.SetupUi
    with lila.pref.PrefUi
    with GameHelper
    with UserHelper
    with SecurityHelper
    with TeamHelper:

  export lila.core.lilaism.Lilaism.{ *, given }
  export lila.common.extensions.*
  export lila.ui.Icon
  export lila.web.Nonce
  export lila.web.ui.{ PageModule, EsmList }
  export lila.api.Context.{ ctxToTranslate as _, *, given }
  export lila.api.PageData

  private var envVar: Option[Env] = None
  def setEnv(e: Env)              = envVar = Some(e)
  def env: Env                    = envVar.get

  def netConfig           = env.net
  def netBaseUrl          = env.net.baseUrl
  def contactEmailInClear = env.net.email.value
  def picfitUrl           = env.memo.picfitUrl

  given lila.core.config.NetDomain              = env.net.domain
  given (using ctx: PageContext): Option[Nonce] = ctx.nonce

  lazy val siteName: String =
    if env.net.siteName == "localhost:9663" then "lichess.dev"
    else env.net.siteName
  lazy val siteNameFrag: Frag =
    if siteName == "lichess.org" then frag("lichess", span(".org"))
    else frag(siteName)

  def apiVersion = lila.security.Mobile.Api.currentVersion

  def explorerEndpoint       = env.explorerEndpoint
  def tablebaseEndpoint      = env.tablebaseEndpoint
  def externalEngineEndpoint = env.externalEngineEndpoint

  def chessground(pov: Pov)(using ctx: Context): Frag =
    chessground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove
        .map(_.origDest)
        .so: (orig, dest) =>
          List(orig, dest),
      blindfold = pov.player.blindfold,
      pref = ctx.pref
    )

  // helper dependencies

  val numberHelper = lila.ui.NumberHelper
  export numberHelper.*

  val i18nHelper = lila.ui.I18nHelper(lila.i18n.JsDump, lila.i18n.Translator)
  export i18nHelper.{ given, * }

  val stringHelper = wire[lila.ui.StringHelper]
  export stringHelper.*

  val dateHelper = wire[lila.ui.DateHelper]
  export dateHelper.*

  val flashHelper = wire[lila.web.ui.FlashHelper]
  export flashHelper.*

  def flairApi        = env.user.flairApi
  lazy val formHelper = wire[lila.ui.FormHelper]
  export formHelper.*

  def routeTournamentShow: String => Call = controllers.routes.Tournament.show
  def getTourName                         = env.tournament.getTourName
  def defaultTranslate                    = lila.i18n.Translator.toDefault
  lazy val tourHelper                     = wire[lila.tournament.ui.TournamentHelper]
  export tourHelper.*

  lazy val assetHelper = AssetHelper(
    i18nHelper = i18nHelper,
    net = netConfig,
    manifest = env.web.manifest,
    explorerEndpoint = env.explorerEndpoint,
    tablebaseEndpoint = env.tablebaseEndpoint,
    externalEngineEndpoint = env.externalEngineEndpoint
  )
  export assetHelper.{ *, given }

  export ChessHelper.*
  export lila.ui.HtmlHelper.*

  def titleOrText(v: String)(using ctx: Context): Modifier = titleOrTextFor(ctx.blind, v)

  def isChatPanicEnabled = env.chat.panic.enabled

  def blockingReportScores: (Int, Int, Int) = (
    env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0),
    env.report.scoreThresholdsSetting.get().mid,
    env.report.scoreThresholdsSetting.get().high
  )
