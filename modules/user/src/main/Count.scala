package lila.user

private[user] case class Count(
    ai: Int,
    draw: Int,
    drawH: Int, // only against human opponents
    game: Int,
    loss: Int,
    lossH: Int, // only against human opponents
    rated: Int,
    win: Int,
    winH: Int) { // only against human opponents

  def gameH = winH + lossH + drawH
}

private[user] object Count {

  import lila.db.JsTube
  import play.api.libs.json.Json

  val default = Count(0, 0, 0, 0, 0, 0, 0, 0, 0)

  private[user] lazy val tube = JsTube[Count](
    Json.reads[Count], 
    Json.writes[Count])
}
