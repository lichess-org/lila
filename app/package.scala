package lila

import scalaz.Zero
import play.api.mvc.{ Result, Results }

package object app extends PackageObject with WithPlay {

  implicit val LilaResultZero = new Zero[Result] {
    val zero = Results.NotFound
  }
}
