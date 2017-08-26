package lila.common

import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.api.routing.Router

final class LilaComponents(context: ApplicationLoader.Context, val router: Router)
  extends BuiltInComponentsFromContext(context) {

  def httpFilters = Nil
}
