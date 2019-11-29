package lila

import reactivemongo.api.commands.WriteResult
import reactivemongo.api.ReadPreference
import io.methvin.play.autoconfig._

package object db extends PackageObject {

  type RunCommand = (dsl.Bdoc, ReadPreference) => Fu[dsl.Bdoc]

  def recoverDuplicateKey[A](f: WriteResult => A): PartialFunction[Throwable, A] = {
    case wr: WriteResult if isDuplicateKey(wr) => f(wr)
  }

  def isDuplicateKey(wr: WriteResult) = wr.code.contains(11000)

  case class DbConfig(
      uri: String,
      @ConfigName("image.collection") imageCollName: Option[String]
  )

  private[db] def logger = lila.log("db")
}
