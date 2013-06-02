package lila.user

case class Count(
    game: Int,
    rated: Int,
    win: Int,
    loss: Int,
    draw: Int,
    winH: Int, // only against human opponents
    lossH: Int, // only against human opponents
    drawH: Int, // only against human opponents
    ai: Int) {

  def gamesH = winH + lossH + drawH
}

object Count {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[user] lazy val tube = Tube[Count](Json.reads[Count], Json.writes[Count])
}
