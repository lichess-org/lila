package lila
package message

import user.User

import scalaz.effects._
import com.github.ornicar.paginator._
import scala.math.ceil

final class Api(
  threadRepo: ThreadRepo, 
  maxPerPage: Int) {
}
