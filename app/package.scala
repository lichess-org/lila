package lila

import scalaz.Zero
import play.api.mvc.{ Result, Results }

package object app extends PackageObject with WithPlay with socket.WithSocket {

  implicit val LilaResultZero = new Zero[Result] {
    val zero = Results.NotFound
  }
}
