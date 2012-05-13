package lila
package templating

import model.User

import play.api.mvc.RequestHeader

object Environment
    extends scalaz.Identitys
    with StringHelper
    with AssetHelper
    with I18nHelper
    with UiHelper
    with RequestHelper {

  type Me = Option[User]

  type Req = RequestHeader
}
