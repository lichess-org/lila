package lila.core
package chat

object panic:

  type IsAllowed = UserId => (UserId => Fu[Option[user.User]]) => Fu[Boolean]
