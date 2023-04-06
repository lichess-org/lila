package lila.relay

import play.api.data.Forms.*
import chess.format.pgn.{ Tag, Tags }

// used to change names and ratings of broadcast players
case class RelayPlayer(name: String, rating: Option[Int])

case class RelayPlayers(text: String):

  lazy val players: Map[String, RelayPlayer] = text.linesIterator
    .take(1000)
    .toList
    .flatMap { line =>
      line.split(';').map(_.trim) match
        case Array(id, name, rating) => Some(id -> RelayPlayer(name, rating.toIntOption))
        case Array(id, name)         => Some(id -> RelayPlayer(name, none))
        case _                       => none
    }
    .toMap

  def update(games: RelayGames): RelayGames = games.map { game =>
    game.copy(tags = update(game.tags))
  }

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags) { case (tags, color) =>
      tags ++ Tags {
        tags(color.name).flatMap(players.get) ?? { rp =>
          List(
            Tag(color.fold(Tag.White, Tag.Black), rp.name).some,
            rp.rating.map { rating => Tag(color.fold(Tag.WhiteElo, Tag.BlackElo), rating.toString) }
          ).flatten
        }
      }
    }
