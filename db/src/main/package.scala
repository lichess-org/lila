package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  private[db] def fuck(msg: Any): Funit = fufail(new DbException(msg.toString))

  import Types._

  trait InColl[A] { implicit def coll: Coll }

  type TubeInColl[A] = Tube[A] with InColl[A]

  implicit def inColl[A](c: Coll): InColl[A] = new InColl[A] {
    def coll = c
  }
}
