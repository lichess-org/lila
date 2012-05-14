package lila

import ornicar.scalalib._

package object game {

  type ValidIOEvents = Valid[scalaz.effects.IO[List[Event]]]
}
