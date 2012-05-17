package lila
package user

import core.CoreEnv

trait UserHelper {

  protected def env: CoreEnv

  private def cached = env.user.cached

  def userIdToUsername(userId: String) = cached username userId
}
