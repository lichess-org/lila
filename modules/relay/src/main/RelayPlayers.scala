package lila.relay

import play.api.data.Forms.*
import chess.format.pgn.{ Tag, Tags }

// used to change names and ratings of broadcast players
private case class RelayPlayer(name: String, rating: Option[Int], title: Option[UserTitle])

private case class RelayPlayers(text: String):

  lazy val players: Map[String, RelayPlayer] = text.linesIterator
    .take(1000)
    .toList
    .flatMap: line =>
      line.split(';').map(_.trim) match
        case Array(id, name, rating, title) =>
          Some(id -> RelayPlayer(name, rating.toIntOption, lila.user.Title.get(title)))
        case Array(id, name, rating) => Some(id -> RelayPlayer(name, rating.toIntOption, none))
        case Array(id, name)         => Some(id -> RelayPlayer(name, none, none))
        case _                       => none
    .toMap

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        tags(color.name).flatMap(players.get) so { rp =>
          List(
            Tag(color.fold(Tag.White, Tag.Black), rp.name).some,
            rp.rating.map { rating => Tag(color.fold(Tag.WhiteElo, Tag.BlackElo), rating.toString) },
            rp.title.map { title => Tag(color.fold(Tag.WhiteTitle, Tag.BlackTitle), title.value) }
          ).flatten
        }
