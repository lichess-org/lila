package lila.user

import lila.db.JsTube
import play.api.libs.json._

case class SubElo(nb: Int, elo: Int) {

  def countRated = nb // for compat with chess elo calculator

  def addGame(newElo: Int) = SubElo(nb = nb + 1, elo = newElo)

  def withElo(e: Int) = copy(elo = e)
}

case object SubElo {

  val default = SubElo(0, User.STARTING_ELO)

  private[user] lazy val tube = JsTube[SubElo](Json.reads[SubElo], Json.writes[SubElo])
}

