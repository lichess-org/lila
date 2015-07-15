package lila

import reactivemongo.api._

package object db extends PackageObject with WithPlay {

  type TubeInColl[A] = Tube[A] with InColl[A]

  type JsTubeInColl[A] = JsTube[A] with InColl[A]
  type BsTubeInColl[A] = BsTube[A] with InColl[A]

  private val duplicateKeyMessage = "duplicate key error"

  import reactivemongo.api.commands.WriteResult

  def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] = {
    case e: WriteResult if e.getMessage.contains(duplicateKeyMessage) => f(e)
  }
}
