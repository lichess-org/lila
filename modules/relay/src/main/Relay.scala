package lila.relay

import org.joda.time.DateTime

import ornicar.scalalib.Random

case class Relay(
    id: String, // random ID
    ficsId: Int,
    name: String,
    status: Relay.Status,
    date: DateTime,
    games: List[Relay.Game],
    enabled: Boolean) {

  lazy val splitName = name split " - " toList

  def baseName = (splitName lift 0) | name

  def extName: Option[String] = splitName.tailOption
    .map(_ mkString " - ")
    .filter(_.nonEmpty)

  def gameByFicsId(ficsId: Int) = games.find(_.ficsId == ficsId)

  def gameIdByFicsId(ficsId: Int) = gameByFicsId(ficsId).map(_.id)

  def gameIds = games.map(_.id)

  def activeGames = games.filterNot(_.end)

  def finished = status == Relay.Status.Finished || activeGames.isEmpty

  lazy val slug = mkSlug(name)

  lazy val baseSlug = mkSlug(baseName)

  private def mkSlug(str: String) = Relay.SlugR.replaceAllIn(lila.common.String slugify str, "-")
}

object Relay {

  private val SlugR = """-{2,}""".r

  def make(ficsId: Int, name: String, status: Status) = Relay(
    id = Random nextStringUppercase 8,
    ficsId = ficsId,
    name = name,
    status = status,
    date = DateTime.now,
    games = Nil,
    enabled = !name.split(' ').contains("test"))

  case class Mini(id: String, name: String, slug: String)
  object Mini {
    def apply(relay: Relay): Mini = Mini(relay.id, relay.baseName, relay.slug)
  }

  case class Round(relay: Relay, otherGames: List[lila.game.Game])

  case class WithContent(relay: Relay, content: Option[Content])

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
