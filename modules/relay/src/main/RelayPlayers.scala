package lila.relay

import play.api.data.Forms.*
import chess.format.pgn.{ Tag, Tags }
import chess.FideId

// used to change names and ratings of broadcast players
private case class RelayPlayer(
    name: Option[PlayerName],
    rating: Option[Int],
    title: Option[UserTitle],
    fideId: Option[FideId] = none
)
object RelayPlayer:
  type Token = String
  private val splitRegex = """\W""".r
  def tokenize(name: PlayerName): Token =
    splitRegex
      .split(name.toLowerCase.trim)
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .sorted
      .mkString(" ")

private class RelayPlayers(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  private lazy val players: Map[PlayerName, RelayPlayer] =
    val lines = text.linesIterator
    lines.nonEmpty.so:
      val parse = parser.pick(lines.next)
      text.linesIterator.take(1000).toList.flatMap(parse).toMap

  private lazy val tokenizedPlayers: Map[RelayPlayer.Token, RelayPlayer] =
    players.mapKeys(RelayPlayer.tokenize)

  private object parser:
    def pick(line: String) = if line.contains(';') then parser.v1 else parser.v2
    // Original name; Replacement name; Optional rating; Optional title
    val v1 = (line: String) =>
      line.split(';').map(_.trim) match
        case Array(id, name, rating, title) =>
          Some(id -> RelayPlayer(name.some, rating.toIntOption, lila.user.Title.get(title)))
        case Array(id, name, rating) => Some(id -> RelayPlayer(name.some, rating.toIntOption, none))
        case Array(id, name)         => Some(id -> RelayPlayer(name.some, none, none))
        case _                       => none
    // Original name / Optional rating / Optional title / Optional replacement name
    val v2 = (line: String) =>
      line.split('=').map(_.trim) match
        case Array(name, fideId) =>
          fideId.toIntOption map: id =>
            name -> RelayPlayer(name.some, none, none, FideId(id).some)
        case _ =>
          val arr = line.split('/').map(_.trim)
          arr lift 0 map: fromName =>
            fromName -> RelayPlayer(
              name = arr.lift(3).filter(_.nonEmpty),
              rating = arr.lift(1).flatMap(_.toIntOption),
              title = arr.lift(2).flatMap(lila.user.Title.get)
            )

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        tags(color.name).flatMap(findMatching) so: rp =>
          rp.fideId match
            case Some(fideId) => List(Tag(_.fideIds(color), fideId.toString))
            case None =>
              List(
                rp.name.map(name => Tag(_.names(color), name)),
                rp.rating.map { rating => Tag(_.elos(color), rating.toString) },
                rp.title.map { title => Tag(_.titles(color), title.value) }
              ).flatten

  private def findMatching(name: PlayerName): Option[RelayPlayer] =
    players.get(name) orElse tokenizedPlayers.get(RelayPlayer.tokenize(name))
