package lila
package http

import model.User

import play.api.mvc.RequestHeader

trait HttpEnvironment {

  type Me = Option[User]

  type Req = RequestHeader
}
