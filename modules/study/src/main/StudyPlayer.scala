package lila.study

import chess.{ ByColor, PlayerName, PlayerTitle, Elo, FideId, Centis }
import chess.format.pgn.Tags
import lila.core.fide.Federation

type Players = ByColor[StudyPlayer]

case class StudyPlayer(
    fideId: Option[FideId],
    title: Option[PlayerTitle],
    name: Option[PlayerName],
    rating: Option[Elo],
    team: Option[String]
):
  def id: Option[StudyPlayer.Id] = fideId.orElse(name)

object StudyPlayer:

  type Id = FideId | PlayerName

  def fromTags(tags: Tags): Option[Players] =
    val names = tags.names
    Option.when(names.exists(_.isDefined)):
      (tags.fideIds, tags.titles, names, tags.elos, tags.teams).mapN(StudyPlayer.apply)

  object json:
    import play.api.libs.json.*
    import lila.common.Json.given
    given (using federations: Federation.ByFideIds): OWrites[StudyPlayer] =
      OWrites: p =>
        Json
          .obj("name" -> p.name)
          .add("title" -> p.title)
          .add("rating" -> p.rating)
          .add("fideId" -> p.fideId)
          .add("fed" -> p.fideId.flatMap(federations.get))
    given (using federations: Federation.ByFideIds): OWrites[ChapterPlayer] =
      OWrites: p =>
        Json.toJsObject(p.player).add("clock" -> p.clock)

case class ChapterPlayer(player: StudyPlayer, clock: Option[Centis]):
  export player.*

object ChapterPlayer:
  def fromTags(tags: Tags, clocks: Chapter.BothClocks): Option[ByColor[ChapterPlayer]] =
    StudyPlayer
      .fromTags(tags)
      .map:
        _.zip(clocks).map(ChapterPlayer.apply)
