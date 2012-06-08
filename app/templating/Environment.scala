package lila
package templating

import core.Global.{ env ⇒ coreEnv } // OMG
import http.HttpEnvironment

object Environment
    extends HttpEnvironment
    with scalaz.Identitys
    with scalaz.Options
    with scalaz.Booleans
    with StringHelper
    with AssetHelper
    with UiHelper
    with RequestHelper
    with SettingHelper
    with ConfigHelper 
    with DateHelper 
    with NumberHelper 
    with JsonHelper
    with PaginatorHelper
    with FormHelper
    with message.MessageHelper
    with round.RoundHelper 
    with game.GameHelper
    with user.UserHelper 
    with forum.ForumHelper
    with security.SecurityHelper
    with i18n.I18nHelper 
    with star.StarHelper {

  protected def env = coreEnv
}
