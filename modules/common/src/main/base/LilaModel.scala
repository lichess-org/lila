package lila.base

import alleycats.Zero
import ornicar.scalalib.newtypes.*
import java.time.Instant

trait LilaModel:

  trait OpaqueInstant[A](using A =:= Instant) extends TotalWrapper[A, Instant]

  trait Percent[A]:
    def value(a: A): Double
    def apply(a: Double): A
  object Percent:
    def of[A](w: TotalWrapper[A, Double]): Percent[A] = new:
      def apply(a: Double): A = w(a)
      def value(a: A): Double = w.value(a)
    def toInt[A](a: A)(using p: Percent[A]): Int = Math.round(p.value(a)).toInt // round to closest

  opaque type GameId = String
  object GameId extends OpaqueString[GameId]:
    def size                              = 8
    private val idRegex                   = """[\w-]{8}""".r
    def validate(id: GameId)              = idRegex matches id.value
    def take(str: String): GameId         = GameId(str take size)
    def from(str: String): Option[GameId] = Some(take(str)).filter(validate)

  opaque type GameFullId = String
  object GameFullId extends OpaqueString[GameFullId]:
    val size                                                      = 12
    def apply(gameId: GameId, playerId: GamePlayerId): GameFullId = s"$gameId$playerId"
    extension (e: GameFullId)
      def gameId: GameId         = GameId.take(e)
      def playerId: GamePlayerId = GamePlayerId(e drop GameId.size)

  // Either a GameId or a GameFullId
  opaque type GameAnyId = String
  object GameAnyId extends OpaqueString[GameAnyId]:
    extension (e: GameAnyId)
      def gameId: GameId                 = GameId.take(e)
      def fullId: Option[GameFullId]     = if e.length == GameFullId.size then Some(e) else None
      def playerId: Option[GamePlayerId] = fullId.map(GameFullId.playerId)

  opaque type GamePlayerId = String
  object GamePlayerId extends OpaqueString[GamePlayerId]:
    val size = 4

  opaque type Win = Boolean
  object Win extends YesNo[Win]

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

  opaque type StudyName = String
  object StudyName extends OpaqueString[StudyName]

  opaque type StudyChapterId = String
  object StudyChapterId extends OpaqueString[StudyChapterId]

  opaque type StudyChapterName = String
  object StudyChapterName extends OpaqueString[StudyChapterName]

  opaque type RelayRoundId = String
  object RelayRoundId extends OpaqueString[RelayRoundId]

  opaque type RelayRoundName = String
  object RelayRoundName extends OpaqueString[RelayRoundName]

  opaque type PuzzleId = String
  object PuzzleId extends OpaqueString[PuzzleId]

  opaque type UserTitle = String
  object UserTitle extends OpaqueString[UserTitle]

  opaque type TourId = String
  object TourId extends OpaqueString[TourId]

  opaque type TeamId = String
  object TeamId extends OpaqueString[TeamId]

  opaque type ChatId = String
  object ChatId extends OpaqueString[ChatId]

  opaque type RoomId = String
  object RoomId extends OpaqueString[RoomId]

  opaque type UblogPostId = String
  object UblogPostId extends OpaqueString[UblogPostId]

  opaque type IntRating = Int
  object IntRating extends OpaqueInt[IntRating]:
    extension (r: IntRating) def applyDiff(diff: IntRatingDiff): IntRating = r + diff.value

  opaque type IntRatingDiff = Int
  object IntRatingDiff extends OpaqueInt[IntRatingDiff]:
    given Zero[IntRatingDiff] = Zero(0)

  opaque type Rating = Double
  object Rating extends OpaqueDouble[Rating]

  opaque type RatingProvisional = Boolean
  object RatingProvisional extends YesNo[RatingProvisional]

  opaque type Rank = Int
  object Rank extends OpaqueInt[Rank]

  opaque type CacheKey = String
  object CacheKey extends OpaqueString[CacheKey]

  opaque type RichText = String
  object RichText extends OpaqueString[RichText]

  opaque type Markdown = String
  object Markdown extends OpaqueString[Markdown]

  opaque type Html = String
  object Html extends OpaqueString[Html]

  opaque type UserAgent = String
  object UserAgent extends OpaqueString[UserAgent]

  opaque type MultiPv = Int
  object MultiPv extends OpaqueInt[MultiPv]

  opaque type Depth = Int
  object Depth extends OpaqueInt[Depth]

  opaque type JsonStr = String
  object JsonStr extends OpaqueString[JsonStr]

  opaque type Crawler = Boolean
  object Crawler extends YesNo[Crawler]
