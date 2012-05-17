package lila
package templating

import core.CoreEnv

trait ConfigHelper {

  protected def env: CoreEnv

  def moretimeSeconds = env.settings.MoretimeSeconds

  def gameAnimationDelay = env.settings.GameAnimationDelay
}
