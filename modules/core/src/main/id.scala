package lila.core

import scalalib.newtypes.OpaqueString

// has to be an object, not a package,
// to makes sure opaque types don't leak out
object id:

  opaque type GameId = String
  object GameId extends OpaqueString[GameId]:
    def size                              = 8
    private val idRegex                   = """[\w-]{8}""".r
    def validate(id: GameId)              = idRegex.matches(id.value)
    def take(str: String): GameId         = GameId(str.take(size))
    def from(str: String): Option[GameId] = Some(take(str)).filter(validate)

  opaque type GameFullId = String
  object GameFullId extends OpaqueString[GameFullId]:
    val size                                                      = 12
    def apply(gameId: GameId, playerId: GamePlayerId): GameFullId = s"$gameId$playerId"
    extension (e: GameFullId)
      def gameId: GameId         = GameId.take(e)
      def playerId: GamePlayerId = GamePlayerId(e.drop(GameId.size))
      def anyId: GameAnyId       = e.into(GameAnyId)

  // Either a GameId or a GameFullId
  opaque type GameAnyId = String
  object GameAnyId extends OpaqueString[GameAnyId]:
    given Conversion[GameId, GameAnyId]     = _.value
    given Conversion[GameFullId, GameAnyId] = _.value
    extension (e: GameAnyId)
      def gameId: GameId                 = GameId.take(e)
      def fullId: Option[GameFullId]     = if e.length == GameFullId.size then Some(e) else None
      def playerId: Option[GamePlayerId] = fullId.map(GameFullId.playerId)

  opaque type GamePlayerId = String
  object GamePlayerId extends OpaqueString[GamePlayerId]:
    val size = 4

  opaque type TourId = String
  object TourId extends OpaqueString[TourId]

  opaque type TourPlayerId = String
  object TourPlayerId extends OpaqueString[TourPlayerId]

  opaque type SwissId = String
  object SwissId extends OpaqueString[SwissId]

  opaque type SimulId = String
  object SimulId extends OpaqueString[SimulId]

  opaque type StudyId = String
  object StudyId extends OpaqueString[StudyId]

  opaque type ForumPostId = String
  object ForumPostId extends OpaqueString[ForumPostId]

  opaque type ForumTopicId = String
  object ForumTopicId extends OpaqueString[ForumTopicId]

  opaque type ForumCategId = String
  object ForumCategId extends OpaqueString[ForumCategId]

  opaque type StudyChapterId = String
  object StudyChapterId extends OpaqueString[StudyChapterId]

  opaque type RelayRoundId = String
  object RelayRoundId extends OpaqueString[RelayRoundId]

  opaque type PuzzleId = String
  object PuzzleId extends OpaqueString[PuzzleId]

  opaque type Flair = String
  object Flair extends OpaqueString[Flair]

  opaque type TeamId = String
  object TeamId extends OpaqueString[TeamId]

  opaque type ChatId = String
  object ChatId extends OpaqueString[ChatId]

  opaque type RoomId = String
  object RoomId extends OpaqueString[RoomId]

  opaque type UblogPostId = String
  object UblogPostId extends OpaqueString[UblogPostId]

  opaque type ReportId = String
  object ReportId extends OpaqueString[ReportId]

  opaque type ImageId = String
  object ImageId extends OpaqueString[ImageId]

  opaque type RelayTourId = String
  object RelayTourId extends OpaqueString[RelayTourId]

  opaque type ChallengeId = String
  object ChallengeId extends OpaqueString[ChallengeId]

  opaque type ClasId = String
  object ClasId extends OpaqueString[ClasId]

  opaque type ClasInviteId = String
  object ClasInviteId extends OpaqueString[ClasInviteId]

  opaque type StudentId = String
  object StudentId extends OpaqueString[StudentId]

  opaque type AppealId = String
  object AppealId extends lila.core.userId.OpaqueUserId[AppealId]

  opaque type CmsPageId = String
  object CmsPageId extends OpaqueString[CmsPageId]

  opaque type CmsPageKey = String
  object CmsPageKey extends OpaqueString[CmsPageKey]

  opaque type TitleRequestId = String
  object TitleRequestId extends OpaqueString[TitleRequestId]
