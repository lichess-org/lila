package lila

import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult

package object db extends PackageObject {

  type RunCommand = (dsl.Bdoc, ReadPreference) => Fu[dsl.Bdoc]

  def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] = {
    case wr: WriteResult if isDuplicateKey(wr) => f(wr)
  }

  def isDuplicateKey(wr: WriteResult) = wr.code.contains(11000)

  private[db] def logger = lila.log("db")
}
