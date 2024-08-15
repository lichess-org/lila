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

  case class WithFed(player: StudyPlayer, fed: Option[Federation.Id]):
    export player.*

  type Id = FideId | PlayerName

  def fromTags(tags: Tags): Option[Players] =
    val names = tags.names
    Option.when(names.exists(_.isDefined)):
      (tags.fideIds, tags.titles, names, tags.elos, tags.teams).mapN(StudyPlayer.apply)

  object json:
    import play.api.libs.json.*
    import lila.common.Json.given
    given studyPlayerWithFedWrites: OWrites[StudyPlayer.WithFed] =
      OWrites: p =>
        Json
          .obj("name" -> p.name)
          .add("title" -> p.title)
          .add("rating" -> p.rating)
          .add("fideId" -> p.fideId)
          .add("fed" -> p.fed)
    given chapterPlayerWrites: OWrites[ChapterPlayer] = OWrites: p =>
      Json.toJsObject(p.studyPlayer).add("clock" -> p.clock)

case class ChapterPlayer(player: StudyPlayer, fed: Option[Federation.Id], clock: Option[Centis]):
  export player.*
  def studyPlayer = StudyPlayer.WithFed(player, fed)

object ChapterPlayer:

  def fromTags(tags: Tags, clocks: Chapter.BothClocks): Option[ByColor[ChapterPlayer]] =
    StudyPlayer
      .fromTags(tags)
      .map:
        _.zip(clocks).map: (player, clock) =>
          ChapterPlayer(player, fed = none, clock)
