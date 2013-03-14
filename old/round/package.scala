package lila.app

import ornicar.scalalib._

package object round {

  type ValidIOEvents = Valid[scalaz.effects.IO[List[Event]]]
}
