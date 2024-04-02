package lila.relay

import chess.ByColor
import chess.format.pgn.{ Tag, Tags }

import lila.db.dsl.*
import lila.core.fide.{ Player, FideTC }

final private class RelayFidePlayerApi(guessPlayer: lila.core.fide.GuessPlayer)(using Executor):

  def enrichGames(tour: RelayTour)(games: RelayGames): Fu[RelayGames] =
    val tc = guessTimeControl(tour) | FideTC.standard
    games.traverse: game =>
      enrichTags(game.tags, tc).map: tags =>
        game.copy(tags = tags)

  private def enrichTags(tags: Tags, tc: FideTC): Fu[Tags] =
    (tags.fideIds
      .zip(tags.names)
      .zip(tags.titles))
      .traverse:
        case ((fideId, name), title) => guessPlayer(fideId, name, title)
      .map:
        update(tags, tc, _)

  private def guessTimeControl(tour: RelayTour): Option[FideTC] =
    tour.description
      .split('|')
      .lift(2)
      .map(_.trim.toLowerCase.replace("classical", "standard"))
      .so: tcStr =>
        FideTC.values.find(tc => tcStr.contains(tc.toString))

  private def update(tags: Tags, tc: FideTC, fidePlayers: ByColor[Option[Player]]): Tags =
    chess.Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        fidePlayers(color).so: fide =>
          List(
            Tag(_.fideIds(color), fide.id.toString).some,
            Tag(_.names(color), fide.name).some,
            fide.title.map { title => Tag(_.titles(color), title.value) },
            fide.ratingOf(tc).map { rating => Tag(_.elos(color), rating.toString) }
          ).flatten
