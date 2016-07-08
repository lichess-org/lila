package lila.soclog
package oauth1

import lila.user.User

sealed trait OAuth1Result

object OAuth1Result {

  case object Nope extends OAuth1Result

  case class Authenticated(user: User) extends OAuth1Result

  case class PickUsername(oAuth: OAuth1) extends OAuth1Result
}
