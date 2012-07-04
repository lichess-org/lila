package lila
package templating

import core.CoreEnv

trait ConfigHelper {

  protected def env: CoreEnv

  def moretimeSeconds = env.settings.RoundMoretime

  def gameAnimationDelay = env.settings.RoundAnimationDelay
}
