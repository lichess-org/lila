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
    with ShogigroundHelper {

  // #TODO holy shit fix me
  // requires injecting all the templates!!
  private var envVar: Option[Env] = None
  def setEnv(e: Env) = { envVar = Some(e) }
  def destroy() = { envVar = None }
  def env: Env = envVar.get

  type FormWithCaptcha = (play.api.data.Form[_], lila.common.Captcha)

  def netBaseUrl          = env.net.baseUrl.value
  def isGloballyCrawlable = env.net.crawlable

  lazy val netDomain = env.net.domain
  def isProd         = env.isProd
  def isStage        = env.isStage

  def apiVersion = lila.api.Mobile.Api.currentVersion

  lazy val explorerEndpoint  = env.explorerEndpoint
  lazy val tablebaseEndpoint = env.tablebaseEndpoint

  def contactEmail = env.net.email.value

  def contactEmailLink = a(href := s"mailto:$contactEmail")(contactEmail)

  def isChatPanicEnabled = env.chat.panic.enabled

  def blockingReportNbOpen: Int = env.report.api.nbOpen.awaitOrElse(20.millis, "nbReports", 0)

  val spinner: Frag = raw(
    """<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>"""
  )
}
