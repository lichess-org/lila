package lila
package templating

import http.{ HttpEnvironment, Setting }

object Environment
    extends HttpEnvironment
    with scalaz.Identitys
    with StringHelper
    with AssetHelper
    with I18nHelper
    with UiHelper
    with RequestHelper
    with SettingHelper {

}
