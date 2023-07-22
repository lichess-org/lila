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
    with ChessgroundHelper:

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

  val spinner: Frag = raw(
    """<div class="spinner"><svg viewBox="-2 -2 54 54"><g mask="url(#mask)" fill="none"><path id="a" stroke-width="3.779" d="m21.78 12.64c-1.284 8.436 8.943 12.7 14.54 17.61 3 2.632 4.412 4.442 5.684 7.93"/><path id="b" stroke-width="4.157" d="m43.19 36.32c2.817-1.203 6.659-5.482 5.441-7.623-2.251-3.957-8.883-14.69-11.89-19.73-0.4217-0.7079-0.2431-1.835 0.5931-3.3 1.358-2.38 1.956-5.628 1.956-5.628"/><path id="c" stroke-width="4.535" d="m37.45 2.178s-3.946 0.6463-6.237 2.234c-0.5998 0.4156-2.696 0.7984-3.896 0.6388-17.64-2.345-29.61 14.08-25.23 27.34 4.377 13.26 22.54 25.36 39.74 8.666"/></g></svg></div>"""
  )
