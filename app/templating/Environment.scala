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
    with round.RoundHelper 
    with game.GameHelper
    with user.UserHelper 
    with security.SecurityHelper
    with i18n.I18nHelper {

  protected def env = coreEnv
}
