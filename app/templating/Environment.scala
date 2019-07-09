package lila.app
package templating

import scala.concurrent.duration._

import lila.api.Context
import lila.api.Env.{ current => apiEnv }
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
  with ChessgroundHelper {

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

  def contactEmailLink = a(href := s"mailto:$contactEmail")(contactEmail)

  def cspEnabled = apiEnv.cspEnabledSetting.get _

  def isChatPanicEnabled =
    lila.chat.Env.current.panic.enabled

  def reportNbOpen: Int =
    lila.report.Env.current.api.nbOpen.awaitOrElse(10.millis, 0)

  def NotForKids(f: => Frag)(implicit ctx: Context) = if (ctx.kid) emptyFrag else f

  val spinner: Frag = raw("""<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>""")

  def maybeRemoteSocketDomain(implicit ctx: Context): Option[String] =
    ctx.userId exists Env.socket.socketRemoteUsersSetting.get().matches option remoteSocketDomain

  def usesServiceWorker(implicit ctx: Context): Boolean =
    ctx.userId exists Env.api.serviceWorkerSetting.get().matches
}
