package lila.base

trait LilaModel extends NewTypes:

  opaque type UserId = String
  object UserId extends OpaqueString[UserId]

  opaque type GameId <: String = String
  object GameId extends OpaqueString[GameId]

  opaque type GameFullId <: String = String
  object GameFullId extends OpaqueString[GameFullId]

  opaque type GamePlayerId <: String = String
  object GamePlayerId extends OpaqueString[GamePlayerId]

  opaque type TourPlayerId <: String = String
  object TourPlayerId extends OpaqueString[TourPlayerId]

  opaque type SwissId <: String = String
  object SwissId extends OpaqueString[SwissId]

  opaque type StudyId = String
  object StudyId extends OpaqueString[StudyId]

  opaque type StudyName = String
  object StudyName extends OpaqueString[StudyName]

  opaque type StudyChapterId <: String = String
  object StudyChapterId extends OpaqueString[StudyChapterId]

  opaque type StudyChapterName <: String = String
  object StudyChapterName extends OpaqueString[StudyChapterName]

  opaque type PuzzleId <: String = String
  object PuzzleId extends OpaqueString[PuzzleId]

  opaque type UserTitle <: String = String
  object UserTitle extends OpaqueString[UserTitle]

  opaque type TourId = String
  object TourId extends OpaqueString[TourId]
