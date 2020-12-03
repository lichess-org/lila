package lila.fishnet

import ornicar.scalalib.Random
import com.gilt.gfc.semver.SemVer
import lila.common.IpAddress
import scala.util.{ Failure, Success, Try }

import org.joda.time.DateTime

case class Client(
    _id: Client.Key,                   // API key used to authenticate and assign move or analysis
    userId: Client.UserId,             // lichess user ID
    skill: Client.Skill,               // what can this client do
    instance: Option[Client.Instance], // last seen instance
    enabled: Boolean,
    createdAt: DateTime
) {

  def key = _id

  def fullId = s"$userId:$key"

  def updateInstance(i: Client.Instance): Option[Client] =
    instance.fold(i.some)(_ update i) map { newInstance =>
      copy(instance = newInstance.some)
    }

  def lichess = userId.value == lila.user.User.lichessId

  def offline = key == Client.offline.key

  def disabled = !enabled

  override def toString = s"$key by $userId"
}

object Client {

  val offline = Client(
    _id = Key("offline"),
    userId = UserId("offline"),
    skill = Skill.All,
    instance = None,
    enabled = true,
    createdAt = DateTime.now
  )

  case class Key(value: String)     extends AnyVal with StringValue
  case class Version(value: String) extends AnyVal with StringValue
  case class Python(value: String)  extends AnyVal with StringValue
  case class UserId(value: String)  extends AnyVal with StringValue
  case class Engine(name: String)
  case class Engines(stockfish: Engine)

  case class Instance(
      version: Version,
      engines: Engines,
      ip: IpAddress,
      seenAt: DateTime
  ) {

    def update(i: Instance): Option[Instance] =
      if (i.version != version) i.some
      else if (i.engines != engines) i.some
      else if (i.ip != ip) i.some
      else if (i.seenAt isAfter seenAt.plusMinutes(5)) i.some
      else none

    def seenRecently = seenAt isAfter Instance.recentSince
  }

  object Instance {

    def recentSince = DateTime.now.minusMinutes(15)
  }

  sealed trait Skill {
    def key = toString.toLowerCase
  }
  object Skill {
    case object Move     extends Skill
    case object Analysis extends Skill
    case object All      extends Skill
    val all                = List(Move, Analysis, All)
    def byKey(key: String) = all.find(_.key == key)
  }

  final class ClientVersion(minVersionString: String) {

    val minVersion = SemVer(minVersionString)

    def accept(v: Client.Version): Try[Unit] =
      Try(SemVer(v.value)) match {
        case Success(version) if version >= minVersion => Success(())
        case Success(_) =>
          Failure(
            new Exception(
              s"Version $v is no longer supported. Please restart fishnet to upgrade."
            )
          )
        case Failure(error) => Failure(error)
      }
  }

  def makeKey = Key(Random.secureString(8))
}
