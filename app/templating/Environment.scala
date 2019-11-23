package lidraughts.app
package templating

import scala.concurrent.duration._

import play.twirl.api.Html

import lidraughts.api.Env.{ current => apiEnv }

object Environment
  extends lidraughts.Lidraughtsisms
  with StringHelper
  with HtmlHelper
  with JsonHelper
  with AssetHelper
  with RequestHelper
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
  with AnalysisHelper
  with TournamentHelper
  with SimulHelper
  with DraughtsgroundHelper
  with ui.ScalatagsTwirl {

  type FormWithCaptcha = (play.api.data.Form[_], lidraughts.common.Captcha)

  def netDomain = apiEnv.Net.Domain
  def netBaseUrl = apiEnv.Net.BaseUrl
  val isGloballyCrawlable = apiEnv.Net.Crawlable

  def isProd = apiEnv.isProd
  def isStage = apiEnv.isStage

  def apiVersion = lidraughts.api.Mobile.Api.currentVersion

  def explorerEndpoint = apiEnv.ExplorerEndpoint

  def tablebaseEndpoint = apiEnv.TablebaseEndpoint

  def contactEmail = apiEnv.Net.Email

  def contactEmailLink = Html(s"""<a href="mailto:$contactEmail">$contactEmail</a>""")

  def reportNbOpen: Int =
    lidraughts.report.Env.current.api.nbOpen.awaitOrElse(10.millis, 0)

  def cspEnabled = apiEnv.cspEnabledSetting.get _

  def isChatPanicEnabled =
    lidraughts.chat.Env.current.panic.enabled

  def NotForKids(f: => Html)(implicit ctx: lidraughts.api.Context) = if (ctx.kid) emptyHtml else f
}
