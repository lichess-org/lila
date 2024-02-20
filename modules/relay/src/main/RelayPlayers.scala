package lila.relay

import play.api.data.Forms.*
import chess.format.pgn.{ Tag, Tags }

// used to change names and ratings of broadcast players
private case class RelayPlayer(name: PlayerName, rating: Option[Int], title: Option[UserTitle])

private class RelayPlayers(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  lazy val players: Map[PlayerName, RelayPlayer] =
    val lines = text.linesIterator
    lines.nonEmpty.so:
      val parse = parser.pick(lines.next)
      text.linesIterator.take(1000).toList.flatMap(parse).toMap

  private object parser:
    def pick(line: String) = if line.contains(';') then parser.v1 else parser.v2
    // Original name; Replacement name; Optional rating; Optional title
    val v1 = (line: String) =>
      line.split(';').map(_.trim) match
        case Array(id, name, rating, title) =>
          Some(id -> RelayPlayer(name, rating.toIntOption, lila.user.Title.get(title)))
        case Array(id, name, rating) => Some(id -> RelayPlayer(name, rating.toIntOption, none))
        case Array(id, name)         => Some(id -> RelayPlayer(name, none, none))
        case _                       => none
    // Original name / Optional rating / Optional title / Optional replacement name
    val v2 = (line: String) =>
      val arr = line.split('/').map(_.trim)
      arr lift 0 map: fromName =>
        fromName -> RelayPlayer(
          name = arr.lift(3).filter(_.nonEmpty) | fromName,
          rating = arr.lift(1).flatMap(_.toIntOption),
          title = arr.lift(2).flatMap(lila.user.Title.get)
        )

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        tags(color.name).flatMap(players.get) so: rp =>
          List(
            Tag(color.fold(Tag.White, Tag.Black), rp.name).some,
            rp.rating.map { rating => Tag(color.fold(Tag.WhiteElo, Tag.BlackElo), rating.toString) },
            rp.title.map { title => Tag(color.fold(Tag.WhiteTitle, Tag.BlackTitle), title.value) }
          ).flatten
