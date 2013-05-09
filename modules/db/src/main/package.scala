package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  type TubeInColl[A] = Tube[A] with InColl[A]
}
