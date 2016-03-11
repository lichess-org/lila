package lila.fishnet

import org.joda.time.DateTime

case class Client(
    _id: Client.Key, // API key used to authenticate and assign move or analysis
    version: Client.Version, // 0.0.1
    userId: Client.UserId, // lichess user ID
    skill: Client.Skill, // what can this client do
    enabled: Boolean,
    stats: Stats,
    createdAt: DateTime,
    lastSeenAt: Option[DateTime]) {

  def key = _id
}

object Client {

  case class Key(value: String) extends AnyVal
  case class Version(value: String) extends AnyVal
  case class UserId(value: String) extends AnyVal

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
