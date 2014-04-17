package lila.app
package templating

import ornicar.scalalib
import play.api.templates.Html

import lila.api.Env.{ current => apiEnv }

object Environment
    extends scalaz.syntax.ToIdOps
    with scalaz.std.OptionInstances
    with scalaz.std.OptionFunctions
    with scalaz.std.StringInstances
    with scalaz.syntax.std.ToOptionIdOps
    with scalalib.OrnicarMonoid.Instances
    with scalalib.Zero.Instances
    with scalalib.OrnicarOption
    with lila.BooleanSteroids
    with lila.OptionSteroids
    with StringHelper
    with JsonHelper
    with MarkdownHelper
    with AssetHelper
    with RequestHelper
    with DateHelper
    with NumberHelper
    with PaginatorHelper
    with FormHelper
    with SetupHelper
    with MessageHelper
    with lila.round.RoundHelper
    with AiHelper
    with GameHelper
    with UserHelper
    with ForumHelper
    with I18nHelper
    with BookmarkHelper
    with NotificationHelper
    with SecurityHelper
    with TeamHelper
    with AnalysisHelper
    with IRCHelper
    with TournamentHelper {

  implicit val LilaHtmlMonoid = scalaz.Monoid.instance[Html](
    (a, b) => Html(a.body + b.body),
    Html(""))

  type FormWithCaptcha = (play.api.data.Form[_], lila.common.Captcha)

  def netDomain = apiEnv.Net.Domain
  def netBaseUrl = apiEnv.Net.BaseUrl
  lazy val portsString = (apiEnv.Net.Port :: apiEnv.Net.ExtraPorts) mkString ","

  def isProd = apiEnv.isProd

  lazy val siteMenu = new lila.app.ui.SiteMenu(trans)

  lazy val lobbyMenu = new lila.app.ui.LobbyMenu(trans)
}
