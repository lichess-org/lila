package lidraughts

import reactivemongo.api.commands.WriteResult

package object db extends PackageObject {

  def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] = {
    case e: WriteResult if e.code.contains(11000) => f(e)
  }

  private[db] def logger = lidraughts.log("db")
}
