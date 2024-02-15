package lila.relay

import chess.format.pgn.*

type TeamName = String

private class RelayTeams(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  lazy val teams: Map[TeamName, List[PlayerName]] = text.linesIterator
    .take(1000)
    .toList
    .flatMap: line =>
      line.split(';').map(_.trim) match
        case Array(team, player) => Some(team -> player)
        case _                   => none
    .groupBy(_._1)
    .view
    .mapValues(_.map(_._2))
    .toMap

  private lazy val playerTeams: Map[PlayerName, TeamName] =
    teams.flatMap: (team, players) =>
      players.map(_ -> team)

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags
        .players(color)
        .flatMap(playerTeams.get)
        .fold(tags): team =>
          tags + Tag(color.fold(Tag.WhiteTeam, Tag.BlackTeam), team)
