package lila.app
package templating

import lila.app.ui.ScalatagsTemplate.*

object Environment
    extends StringHelper
    with RouterHelper
    with AssetHelper
    with DateHelper
    with NumberHelper
    with PaginatorHelper
    with FormHelper
    with SetupHelper
    with AiHelper
    with GameHelper
    with UserHelper
    with I18nHelper
    with SecurityHelper
    with TeamHelper
    with TournamentHelper
    with FlashHelper
    with ChessgroundHelper
    with HtmlHelper:

  export lila.Lila.{ id as _, *, given }
  export lila.api.Context.{ *, given }
  export lila.api.{ PageData, Nonce }
  export lila.user.Me
  export lila.common.licon

  private var envVar: Option[Env] = None
  def setEnv(e: Env)              = envVar = Some(e)
  def env: Env                    = envVar.get

  type FormWithCaptcha = (play.api.data.Form[?], lila.common.Captcha)

  def netConfig                      = env.net
  def netBaseUrl                     = env.net.baseUrl.value
  def contactEmailInClear            = env.net.email.value
  given lila.common.config.NetDomain = env.net.domain

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

  def isChatPanicEnabled = env.chat.panic.enabled

  def blockingReportScores: (Int, Int, Int) = (
    env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0),
    env.report.scoreThresholdsSetting.get().mid,
    env.report.scoreThresholdsSetting.get().high
  )
