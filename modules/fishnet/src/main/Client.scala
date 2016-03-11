package lila.fishnet

import org.joda.time.DateTime

case class Client(
    _id: String, // API key used to authenticate and assign move or analysis
    userId: String, // lichess user ID
    skill: Client.Skill,
    enabled: Boolean,
    stats: Stats,
    createdAt: DateTime) {

  def key = _id
}

object Client {

  sealed trait Skill {
    def key = toString.toLowerCase
  }
  object Skill {
    case object Move extends Skill
    case object Analysis extends Skill
    val all = List(Move, Analysis)
  }
}
