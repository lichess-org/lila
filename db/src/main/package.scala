package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  private[db] def fuck(msg: Any): Funit = fufail(new DbException(msg.toString))

  type TubeInColl[A] = Tube[A] with InColl[A]
}
