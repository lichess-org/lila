package lila.app
package templating

import lila.web.ui.ScalatagsTemplate.*
import lila.web.ui.*

object Environment
    extends StringHelper
    with RouterHelper
    with AssetHelper
    with DateHelper
    with NumberHelper
    with PaginatorHelper
    with FormHelper
    with lila.setup.SetupUi
    with lila.pref.PrefUi
    with GameHelper
    with UserHelper
    with I18nHelper
    with SecurityHelper
    with TeamHelper
    with TournamentHelper
    with FlashHelper
    with ChessgroundHelper
    with HtmlHelper:

  export lila.core.lilaism.Lilaism.{ *, given }
  export lila.common.extensions.*
  export lila.common.Icon
  export lila.web.Nonce
  export lila.api.Context.{ *, given }
  export lila.api.PageData

  private var envVar: Option[Env] = None
  def setEnv(e: Env)              = envVar = Some(e)
  def env: Env                    = envVar.get

  def netConfig           = env.net
  def netBaseUrl          = env.net.baseUrl.value
  def contactEmailInClear = env.net.email.value

  given lila.core.config.NetDomain = env.net.domain

  def jsDump     = lila.i18n.JsDump
  def translator = lila.i18n.Translator
  def flairApi   = env.user.flairApi
  export lila.mailer.translateDuration

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

  def chessground(pov: lila.game.Pov)(using ctx: Context): Frag =
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

  def titleOrText(v: String)(using ctx: Context): Modifier = titleOrTextFor(ctx.blind, v)

  def isChatPanicEnabled = env.chat.panic.enabled

  def blockingReportScores: (Int, Int, Int) = (
    env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0),
    env.report.scoreThresholdsSetting.get().mid,
    env.report.scoreThresholdsSetting.get().high
  )
