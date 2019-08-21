package lidraughts.draughtsnet

import lidraughts.common.IpAddress

import org.joda.time.DateTime

case class Client(
    _id: Client.Key, // API key used to authenticate and assign move or analysis
    userId: Client.UserId, // lidraughts user ID
    skill: Client.Skill, // what can this client do
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

  def Lidraughts = userId.value == lidraughts.user.User.lidraughtsId

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

  case class Key(value: String) extends AnyVal with StringValue
  case class Version(value: String) extends AnyVal with StringValue
  case class Python(value: String) extends AnyVal with StringValue
  case class UserId(value: String) extends AnyVal with StringValue
  case class Engine(name: String)
  case class Engines(scan: Engine)

  case class Instance(
      version: Version,
      python: Python,
      engines: Engines,
      ip: IpAddress,
      seenAt: DateTime
  ) {

    def update(i: Instance): Option[Instance] =
      if (i.version != version) i.some
      else if (i.python != python) i.some
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
    case object Move extends Skill
    case object Analysis extends Skill
    case object Commentary extends Skill
    case object All extends Skill
    val all = List(Move, Analysis, Commentary, All)
    def byKey(key: String) = all.find(_.key == key)
  }

  def makeKey = Key(scala.util.Random.alphanumeric take 8 mkString)
}
