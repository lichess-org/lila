package lila.relay

import chess.format.pgn.{ Tag, Tags }
import chess.{ Elo, FideId, PlayerName, PlayerTitle }
import play.api.data.Forms.*

import lila.core.fide.{ Player, PlayerToken }

// used to change names and ratings of broadcast players
private case class RelayPlayer(
    name: Option[PlayerName],
    rating: Option[Elo],
    title: Option[PlayerTitle],
    fideId: Option[FideId] = none
)

private class RelayPlayersTextarea(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  private lazy val players: Map[PlayerName, RelayPlayer] =
    val lines = text.linesIterator
    lines.nonEmpty.so:
      text.linesIterator.take(1000).toList.flatMap(parse).toMap

  object tokenize:
    private val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
    private val splitRegex     = """\W""".r
    def apply(str: String): PlayerToken =
      splitRegex
        .split:
          java.text.Normalizer
            .normalize(str.trim, java.text.Normalizer.Form.NFD)
            .replaceAllIn(nonLetterRegex, "")
            .toLowerCase
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .sorted
        .mkString(" ")

  // With tokenized player names
  private lazy val tokenizedPlayers: Map[PlayerToken, RelayPlayer] =
    players.mapKeys(name => tokenize(name.value))

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

  // Original name / Optional rating / Optional title / Optional replacement name
  private def parse(line: String): Option[(PlayerName, RelayPlayer)] =
    line.split('=').map(_.trim) match
      case Array(nameStr, fideId) =>
        val name  = PlayerName(nameStr)
        val parts = fideId.split('/').map(_.trim)
        parts
          .lift(0)
          .flatMap(_.toIntOption)
          .map: id =>
            name -> RelayPlayer(name.some, none, parts.lift(1).flatMap(PlayerTitle.get), FideId(id).some)
      case _ =>
        val arr = line.split('/').map(_.trim)
        arr
          .lift(0)
          .map: fromName =>
            PlayerName(fromName) -> RelayPlayer(
              name = PlayerName.from(arr.lift(3).filter(_.nonEmpty)),
              rating = Elo.from(arr.lift(1).flatMap(_.toIntOption)),
              title = arr.lift(2).flatMap(PlayerTitle.get)
            )

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        tags
          .names(color)
          .flatMap(findMatching)
          .so: rp =>
            List(
              rp.fideId.map(id => Tag(_.fideIds(color), id.toString)),
              rp.name.map(name => Tag(_.names(color), name)),
              rp.rating.map(rating => Tag(_.elos(color), rating.toString)),
              rp.title.map(title => Tag(_.titles(color), title.value))
            ).flatten

  private def findMatching(name: PlayerName): Option[RelayPlayer] =
    players
      .get(name)
      .orElse:
        val token = tokenize(name.value)
        tokenizedPlayers.get(token).orElse(combinationPlayers.get(token))
