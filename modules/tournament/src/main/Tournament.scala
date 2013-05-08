package lila.tournament

sealed trait Tournament {
  def id: String

  def isRunning = false
}

case class Created(id: String) {
}
