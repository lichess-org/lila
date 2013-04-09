package lila.app
package templating

import lila.api.Env.{ current ⇒ apiEnv }

object Environment
    extends scalaz.Identitys
    with scalaz.Options
    with scalaz.Booleans
    with StringHelper
    with MarkdownHelper
    with AssetHelper
    with UiHelper
    with RequestHelper
    with DateHelper 
    with NumberHelper 
    with PaginatorHelper
    with FormHelper
    with SetupHelper
    with MessageHelper
    with RoundHelper 
    with AiHelper
    with GameHelper
    with UserHelper
    with ForumHelper
    with I18nHelper
    with BookmarkHelper
    with NotificationHelper {
  // with tournament.TournamentHelper 
  // with team.TeamHelper {

  def netDomain = apiEnv.Net.Domain
  def netBaseUrl = apiEnv.Net.BaseUrl

  def isDev = apiEnv.isDev
}
