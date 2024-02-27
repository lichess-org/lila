package lila.relay

import play.api.data.Forms.*
import chess.format.pgn.{ Tag, Tags }
import chess.FideId

import lila.fide.{ PlayerName, PlayerToken, FidePlayer }

// used to change names and ratings of broadcast players
private case class RelayPlayer(
    name: Option[PlayerName],
    rating: Option[Int],
    title: Option[UserTitle],
    fideId: Option[FideId] = none
)

private class RelayPlayers(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  private lazy val players: Map[PlayerName, RelayPlayer] =
    val lines = text.linesIterator
    lines.nonEmpty.so:
      val parse = parser.pick(lines.next)
      text.linesIterator.take(1000).toList.flatMap(parse).toMap

  // With tokenized player names
  private lazy val tokenizedPlayers: Map[PlayerToken, RelayPlayer] =
    players.mapKeys(FidePlayer.tokenize)

  // With player names combinations.
  // For example, if the tokenized player name is "A B C D", the combinations will be:
  // A B, A C, A D, B C, B D, C D, A B C, A B D, A C D, B C D
  private lazy val combinationPlayers: Map[PlayerToken, RelayPlayer] =
    tokenizedPlayers.flatMap: (fullToken, player) =>
      val words = fullToken.split(' ').filter(_.sizeIs > 1).toList
      for
        size        <- 2 to words.length.atMost(4)
        combination <- words.combinations(size)
      yield combination.mkString(" ") -> player

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
    players.get(name) orElse:
      val token = FidePlayer.tokenize(name)
      tokenizedPlayers.get(token) orElse combinationPlayers.get(token)
