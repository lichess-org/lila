package lila.study

import chess.{ ByColor, PlayerName, PlayerTitle, FideId, Centis, IntRating }
import chess.format.pgn.{ Tag, Tags }
import lila.core.fide.Federation

case class StudyPlayer(
    fideId: Option[FideId],
    title: Option[PlayerTitle],
    name: Option[PlayerName],
    rating: Option[IntRating],
    team: Option[String]
):
  def id: Option[StudyPlayer.Id] = fideId.filter(_ != FideId(0)).orElse(name)

object StudyPlayer:

  case class WithFed(player: StudyPlayer, fed: Option[Federation.Id]):
    export player.*

  type Id = FideId | PlayerName

  object country:
    val tagNames = ByColor("WhiteCountry", "BlackCountry")
    val tagTypes = tagNames.map(Tag.tagType)
    def feds(tags: Tags): ByColor[Option[String]] =
      ByColor(color => tags.apply(tagNames(color)))

  def fromTags(tags: Tags)(using guessFed: Federation.Guess): Option[ByColor[StudyPlayer.WithFed]] =
    val names = tags.names
    Option.when(names.exists(_.isDefined)):
      val ratings = tags.ratings.map(_.filter(_ > IntRating(0)))
      val players = (tags.fideIds, tags.titles, names, ratings, tags.teams).mapN(StudyPlayer.apply)
      val feds = country.feds(tags).map(_.flatMap(guessFed))
      players.zip(feds).map(StudyPlayer.WithFed.apply)

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
          .add("team" -> p.team)
          .add("fed" -> p.fed)
    given chapterPlayerWrites: OWrites[ChapterPlayer] = OWrites: p =>
      Json.toJsObject(p.studyPlayer).add("clock" -> p.clock)

case class ChapterPlayer(player: StudyPlayer, fed: Option[Federation.Id], clock: Option[Centis]):
  export player.*
  def studyPlayer = StudyPlayer.WithFed(player, fed)

object ChapterPlayer:

  def fromTags(tags: Tags, clocks: Chapter.BothClocks)(using
      Federation.Guess
  ): Option[ByColor[ChapterPlayer]] =
    StudyPlayer
      .fromTags(tags)
      .map:
        _.zip(clocks).map: (player, clock) =>
          ChapterPlayer(player.player, fed = player.fed, clock)
