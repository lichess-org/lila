package lila.app
package templating

import scala.concurrent.duration._

import lila.app.ui.ScalatagsTemplate._

object Environment
    extends lila.Lilaisms
    with StringHelper
    with AssetHelper
    with DateHelper
    with NumberHelper
    with PaginatorHelper
    with FormHelper
    with SetupHelper
    with AiHelper
    with GameHelper
    with UserHelper
    with ForumHelper
    with I18nHelper
    with SecurityHelper
    with TeamHelper
    with TournamentHelper
    with FlashHelper
    with ChessgroundHelper {

  // #TODO holy shit fix me
  // requires injecting all the templates!!
  private var envVar: Option[Env] = None
  def setEnv(e: Env) = { envVar = Some(e) }
  def env: Env = envVar.get

  type FormWithCaptcha = (play.api.data.Form[_], lila.common.Captcha)

  def netConfig           = env.net
  def netBaseUrl          = env.net.baseUrl.value
  def contactEmailInClear = env.net.email.value

  def apiVersion = lila.api.Mobile.Api.currentVersion

  def explorerEndpoint  = env.explorerEndpoint
  def tablebaseEndpoint = env.tablebaseEndpoint

  def isChatPanicEnabled = env.chat.panic.enabled

  def blockingReportScores: (Int, Int, Int) = (
    env.report.api.maxScores.dmap(_.highest).awaitOrElse(50.millis, "nbReports", 0),
    env.report.scoreThresholdsSetting.get().mid,
    env.report.scoreThresholdsSetting.get().high
  )

  val spinner: Frag = raw(
    """<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>"""
  )
}
