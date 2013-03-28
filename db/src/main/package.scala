package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  private[db] def fuck(msg: Any): Funit = fufail(new DbException(msg.toString))

  import Types._

  implicit def collToInColl(c: Coll): InColl = new InColl {
    def coll = c
  }
}
