package lila
package templating

import core.Global.{ env ⇒ coreEnv } // OMG
import http.{ HttpEnvironment, Setting }

object Environment
    extends HttpEnvironment
    with scalaz.Identitys
    with StringHelper
    with AssetHelper
    with I18nHelper
    with UiHelper
    with RequestHelper
    with SettingHelper
    with UserHelper
    with ConfigHelper {

  protected def env = coreEnv
}
