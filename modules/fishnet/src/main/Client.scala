package lila.fishnet

import com.gilt.gfc.semver.SemVer
import scalalib.SecureRandom

import scala.util.{ Failure, Success, Try }

import lila.core.net.IpAddress

case class Client(
    _id: Client.Key, // API key used to authenticate and assign move or analysis
    userId: UserId, // lichess user ID
    skill: Client.Skill, // what can this client do
    instance: Option[Client.Instance], // last seen instance
    enabled: Boolean,
    createdAt: Instant
):

  def key = _id

  def fullId = s"$userId:$key"

  def updateInstance(i: Client.Instance): Option[Client] =
    instance.fold(i.some)(_.update(i)).map { newInstance =>
      copy(instance = newInstance.some)
    }

  def lichess = this.is(UserId.lichess)

  def offline = key == Client.offline.key

  def disabled = !enabled

  override def toString = s"$key by $userId"

object Client:

  given UserIdOf[Client] = _.userId

  val offline = Client(
    _id = Key("offline"),
    userId = UserId("offline"),
    skill = Skill.All,
    instance = None,
    enabled = true,
    createdAt = nowInstant
  )

  opaque type Key = String
  object Key extends OpaqueString[Key]
  opaque type Version = String
  object Version extends OpaqueString[Version]

  case class Instance(version: Version, ip: IpAddress, seenAt: Instant):

    def update(i: Instance): Option[Instance] =
      if i.version != version then i.some
      else if i.ip != ip then i.some
      else if i.seenAt.isAfter(seenAt.plusMinutes(5)) then i.some
      else none

    def seenRecently = seenAt.isAfter(Instance.recentSince)

  object Instance:

    def recentSince = nowInstant.minusMinutes(15)

  enum Skill:
    case Move
    case Analysis
    case All
    def key = this.toString.toLowerCase
  object Skill:
    def byKey(key: String) = values.find(_.key == key)

  final class ClientVersion(minVersionString: String):

    val minVersion = SemVer(minVersionString)

    def accept(v: Client.Version): Try[Unit] =
      Try(SemVer(v.value)) match
        case Success(version) if version >= minVersion => Success(())
        case Success(_) =>
          Failure(
            new Exception(
              s"Version $v is no longer supported. Please restart fishnet to upgrade."
            )
          )
        case Failure(error) => Failure(error)

  def makeKey = Key(SecureRandom.nextString(8))
