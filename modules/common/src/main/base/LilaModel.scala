package lila.base

import alleycats.Zero
import cats.Show

trait LilaModel extends NewTypes:

  trait Percent[A]:
    def apply(a: A): Double
  object Percent:
    def toInt[A](a: A)(using p: Percent[A]) = Math.round(p(a)).toInt // round to closest

  trait IsUserId[U]:
    def apply(a: U): UserId
    extension (a: UserId) inline def is[U](other: U)(using idOf: IsUserId[U]) = a == idOf(other)

  opaque type UserId = String
  object UserId extends OpaqueString[UserId]:
    given IsUserId[UserId]                                                    = _.value
    extension (a: UserId) inline def is[U](other: U)(using idOf: IsUserId[U]) = a == idOf(other)
    inline def fromStr(inline s: String): UserId                              = s.toLowerCase

  // specialized UserIds like Coach.Id
  trait OpaqueUserId[A] extends OpaqueString[A]:
    given IsUserId[A]                          = _.value
    extension (a: A) inline def userId: UserId = a into UserId

  opaque type UserName = String
  object UserName extends OpaqueString[UserName]:
    given IsUserId[UserName]              = _.id
    given Show[UserName]                  = _.value
    extension (n: UserName) inline def id = UserId(n.value.toLowerCase)

  opaque type GameAnyId = String
  object GameAnyId extends OpaqueString[GameAnyId]

  opaque type GameId = String
  object GameId extends OpaqueString[GameId]

  opaque type GameFullId = String
  object GameFullId extends OpaqueString[GameFullId]

  opaque type GamePlayerId = String
  object GamePlayerId extends OpaqueString[GamePlayerId]

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
  object IntRating extends OpaqueInt[IntRating]

  opaque type IntRatingDiff = Int
  object IntRatingDiff extends OpaqueInt[IntRatingDiff]:
    given Zero[IntRatingDiff] = Zero(apply(0))

  opaque type Rating = Double
  object Rating extends OpaqueDouble[Rating]

  opaque type Rank = Int
  object Rank extends OpaqueInt[Rank]

  opaque type CacheKey = String
  object CacheKey extends OpaqueString[CacheKey]

  opaque type RichText = String
  object RichText extends OpaqueString[RichText]
