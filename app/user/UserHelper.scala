package lila
package user

import core.CoreEnv

trait UserHelper {

  protected def env: CoreEnv

  private def cached = env.user.cached
  private def usernameMemo = env.user.usernameMemo

  def userIdToUsername(userId: String) = cached username userId

  def isUsernameOnline(username: String) = usernameMemo get username

  def isUserIdOnline(userId: String) = 
    usernameMemo get userIdToUsername(userId)
}
