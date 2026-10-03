package lila.relay

import chess.format.Fen

/* Try to detect several ways for the input to be wrong */
private object RelayInputSanity:

  def fixGames(games: RelayGames): RelayGames =
    removeTorneloCountries:
      fixDgtKingsInTheCenter:
        removeGamesWithUnknownPlayer(games)

  private def removeGamesWithUnknownPlayer(games: RelayGames): RelayGames =
    games.filterNot: game =>
      game.hasUnknownPlayer || game.isBye

  // DGT puts the kings in the center on game end
  // and sends it as actual moves if the kings were close to the center
  // so we need to remove the bogus king moves
  private def fixDgtKingsInTheCenter(games: RelayGames): RelayGames = games.map: game =>
    game.copy(
      root = game.root.takeMainlineWhile: node =>
        !dgtBoggusKingMoveRegex.matches(node.move.san.value) ||
          !Fen.read(game.variant, node.fen).forall { sit =>
            sit.checkOf(!sit.color).yes // the king that moved is in check
          }
    )

  private val dgtBoggusKingMoveRegex = """^K[de][45]""".r

  // tornelo uses non-standard 2-letter country codes, so we remove them to avoid confusion
  // also chessbase crashes when trying to parse them because some look like roman numerals!
  private def removeTorneloCountries(games: RelayGames): RelayGames = games.map: game =>
    if game.tags(_.Site).exists(_.toLowerCase.contains("tornelo")) then
      val tags = game.tags.map:
        _.filterNot(t => lila.study.StudyPlayer.country.tagTypes.exists(_ == t.name))
      game.copy(tags = tags)
    else game
