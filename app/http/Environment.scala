package lila
package http

import model.User

import play.api.mvc.RequestHeader
import play.api.mvc.Session

trait HttpEnvironment {

  type Me = Option[User]

  type Req = RequestHeader

  type SessionMap = Session => Session
}
