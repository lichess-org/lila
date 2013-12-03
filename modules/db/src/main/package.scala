package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  type TubeInColl[A] = Tube[A] with InColl[A]

  type JsTubeInColl[A] = JsTube[A] with InColl[A]
  type BsTubeInColl[A] = BsTube[A] with InColl[A]
}
