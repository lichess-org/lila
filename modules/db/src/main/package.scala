package lila

import reactivemongo.api._
import reactivemongo.api.commands.WriteResult

package object db extends PackageObject with WithPlay {
  type TubeInColl[A] = Tube[A] with InColl[A]
  type JsTubeInColl[A] = JsTube[A] with InColl[A]
  type BsTubeInColl[A] = BsTube[A] with InColl[A]

  def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] = {
    case e: WriteResult if e.code.contains(11000) => f(e)
  }

  private[db] def logger = lila.log("db")
}
