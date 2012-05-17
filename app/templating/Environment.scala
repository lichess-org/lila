package lila
package templating

import core.Global.{ env ⇒ coreEnv } // OMG
import round.RoundHelper
import game.GameHelper
import user.UserHelper
import http.{ HttpEnvironment, Setting }

object Environment
    extends HttpEnvironment
    with scalaz.Identitys
    with scalaz.Options
    with scalaz.Booleans
    with StringHelper
    with AssetHelper
    with I18nHelper
    with UiHelper
    with RequestHelper
    with SettingHelper
    with ConfigHelper 
    with RoundHelper 
    with GameHelper
    with UserHelper {

  protected def env = coreEnv
}
