package lila.fishnet

import org.joda.time.DateTime

case class Client(
    _id: Client.Key, // API key used to authenticate and assign move or analysis
    userId: Client.UserId, // lichess user ID
    skill: Client.Skill, // what can this client do
    instance: Option[Client.Instance],
    enabled: Boolean,
    stats: Stats,
    createdAt: DateTime) {

  def key = _id

  def fullId = s"$key:$userId"

  def updateInstance(i: Client.Instance): Option[Client] =
    (instance != i) option copy(instance = i.some)

  def acquire(work: Work) = add(work, _.addAcquire)
  def success(work: Work) = add(work, _.addSuccess)
  def timeout(work: Work) = add(work, _.addTimeout)
  def invalid(work: Work) = add(work, _.addInvalid)

  def lichess = userId.value == "lichess"

  private def add(work: Work, update: Stats.ResultUpdate) = copy(stats = stats.add(work, update))
}

object Client {

  case class Key(value: String) extends AnyVal
  case class UUID(value: String) extends AnyVal
  case class Version(value: String) extends AnyVal
  case class UserId(value: String) extends AnyVal
  case class Engine(name: String)

  case class Instance(
    uuid: UUID,
    version: Version,
    engine: Engine,
    seenAt: DateTime)

  sealed trait Skill {
    def key = toString.toLowerCase
  }
  object Skill {
    case object Move extends Skill
    case object Analysis extends Skill
    val all = List(Move, Analysis)
    def byKey(key: String) = all.find(_.key == key)
  }
}
