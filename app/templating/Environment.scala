package lila.app
package templating

import scala.concurrent.duration._

import play.twirl.api.Html

import lila.api.Env.{ current => apiEnv }

object Environment
  extends lila.Lilaisms
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
  with ChessgroundHelper
  with ui.Scalatags {

  type FormWithCaptcha = (play.api.data.Form[_], lila.common.Captcha)

  def netDomain = apiEnv.Net.Domain
  def netBaseUrl = apiEnv.Net.BaseUrl
  val isGloballyCrawlable = apiEnv.Net.Crawlable

  def isProd = apiEnv.isProd
  def isStage = apiEnv.isStage

  def apiVersion = lila.api.Mobile.Api.currentVersion

  def explorerEndpoint = apiEnv.ExplorerEndpoint

  def tablebaseEndpoint = apiEnv.TablebaseEndpoint

  def contactEmail = apiEnv.Net.Email

  def contactEmailLink = Html(s"""<a href="mailto:$contactEmail">$contactEmail</a>""")

  def cspEnabled = apiEnv.cspEnabledSetting.get _

  def wasmxEnabled = apiEnv.wasmxEnabledSetting.get _

  def isChatPanicEnabled =
    lila.chat.Env.current.panic.enabled

  def reportNbOpen: Int =
    lila.report.Env.current.api.nbOpen.awaitOrElse(10.millis, 0)

  def NotForKids[Html](f: => Html)(implicit ctx: lila.api.Context) =
    if (ctx.kid) emptyHtml else f

  def signalBars(v: Int) = Html {
    val bars = (1 to 4).map { b =>
      s"""<i${if (v < b) " class=\"off\"" else ""}></i>"""
    } mkString ""
    val title = v match {
      case 1 => "Poor connection"
      case 2 => "Decent connection"
      case 3 => "Good connection"
      case _ => "Excellent connection"
    }
    s"""<signal data-hint="$title" class="q$v hint--top">$bars</signal>"""
  }
}
