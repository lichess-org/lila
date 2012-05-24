package lila
package security

import user.User

object Granter {

  def apply(permission: Permission)(user: User): Boolean = 
    Permission(user.roles) exists (_ is permission)
}
