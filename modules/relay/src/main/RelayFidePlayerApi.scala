package lila.relay

import chess.{ ByColor, FideId, PlayerTitle }
import chess.format.pgn.{ Tag, Tags }

import lila.core.fide.{ FideTC, Player }

final private class RelayFidePlayerApi(guessPlayer: lila.core.fide.GuessPlayer)(using Executor):

  def enrichGames(tour: RelayTour)(games: RelayGames): Fu[RelayGames] =
    games.traverse: game =>
      enrichTags(game.tags, tour.info.fideTcOrGuess).map: tags =>
        game.copy(tags = tags)

  def enrichTags(tour: RelayTour): Tags => Fu[Tags] =
    tags => enrichTags(tags, tour.info.fideTcOrGuess)

  private def enrichTags(tags: Tags, tc: FideTC): Fu[Tags] =
    (tags.fideIds
      .zip(tags.names)
      .zip(filterSourceTitles(tags)))
      .traverse:
        // use FIDE ID = 0 to prevent guessing the player info based on their name
        case ((Some(fideId), _), _) if fideId == FideId(0) => fuccess(none)
        case ((fideId, name), title) => guessPlayer(fideId, name, title)
      .map:
        update(tags, tc, _)

  private def update(tags: Tags, tc: FideTC, fidePlayers: ByColor[Option[Player]]): Tags =
    Color.all.foldLeft(tags): (tags, color) =>
      tags ++ Tags:
        fidePlayers(color).so: fide =>
          List(
            Tag(_.fideIds(color), fide.id.toString).some,
            Tag(_.names(color), fide.name).some,
            fide.title.map { title => Tag(_.titles(color), title.value) },
            fide.ratingOf(tc).map { rating => Tag(_.elos(color), rating.toString) }
          ).flatten

  private def filterSourceTitles(tags: Tags): ByColor[Option[PlayerTitle]] =
    tags.titles.map(_.filterNot(fideTitles.contains))

  private val fideTitles =
    import PlayerTitle.*
    Set(GM, WGM, IM, WIM, FM, WFM, CM, WCM)
