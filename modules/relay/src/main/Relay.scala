package lila.relay

import org.joda.time.DateTime

import ornicar.scalalib.Random

case class Relay(
    id: String, // random ID
    ficsId: Int,
    name: String,
    status: Relay.Status,
    date: DateTime,
    games: List[Relay.Game]) {

  def baseName = name.takeWhile('-'!=).trim

  def extName = name.dropWhile('-'!=).tail.trim

  def gameByFicsId(ficsId: Int) = games.find(_.ficsId == ficsId)

  def gameIdByFicsId(ficsId: Int) = gameByFicsId(ficsId).map(_.id)

  def gameIds = games.map(_.id)

  def activeGames = games.filterNot(_.end)

  def slug = Relay.SlugR.replaceAllIn(
    lila.common.String slugify name,
    "-")
}

object Relay {

  private val SlugR = """-{2,}""".r

  def make(ficsId: Int, name: String, status: Status) = Relay(
    id = Random nextStringUppercase 8,
    ficsId = ficsId,
    name = name,
    status = status,
    date = DateTime.now,
    games = Nil)

  case class Game(
      id: String, // lichess game ID
      ficsId: Int,
      white: String,
      black: String,
      end: Boolean) {

    def colorOf(name: String) =
      if (name == white) chess.White.some
      else if (name == black) chess.Black.some
      else none
  }

  object Game {
    def make(ficsId: Int, white: String, black: String) = Game(
      id = Random nextStringUppercase 8,
      ficsId = ficsId,
      white = white,
      black = black,
      end = false)
  }

  sealed abstract class Status(val id: Int)
  object Status {
    object Unknown extends Status(0)
    object Created extends Status(10)
    object Started extends Status(20)
    object Finished extends Status(30)
    val all = List(Unknown, Created, Started, Finished)
    def apply(id: Int) = all.find(_.id == id)
  }
}
