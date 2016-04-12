package lila.fishnet

import com.gilt.gfc.semver.SemVer
import scala.util.{ Try, Success, Failure }

object ClientVersion {

  val minVersion = SemVer("1.0.8")

  def accept(v: Client.Version): Try[Unit] = Try(SemVer(v.value)) match {
    case Success(version) if version >= minVersion => Success(())
    case Success(version) => Failure(new Exception(
      s"Version $v is no longer supported. Please restart fishnet to upgrade."
    ))
    case Failure(error) => Failure(error)
  }
}
