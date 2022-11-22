package lila.base

trait LilaModel extends NewTypes:

  opaque type GameId = String
  object GameId extends OpaqueString[GameId]

  opaque type GameFullId <: String = String
  object GameFullId:
    def apply(v: String): GameFullId = v

  opaque type GamePlayerId <: String = String
  object GamePlayerId:
    def apply(v: String): GamePlayerId = v

  opaque type TourPlayerId <: String = String
  object TourPlayerId:
    def apply(v: String): TourPlayerId = v

  opaque type SwissId <: String = String
  object SwissId:
    def apply(v: String): SwissId = v

  opaque type StudyId = String
  object StudyId extends OpaqueString[StudyId]

  opaque type StudyName = String
  object StudyName extends OpaqueString[StudyName]

  opaque type StudyChapterId <: String = String
  object StudyChapterId:
    def apply(v: String): StudyChapterId = v

  opaque type StudyChapterName <: String = String
  object StudyChapterName:
    def apply(v: String): StudyChapterName = v

  opaque type PuzzleId <: String = String
  object PuzzleId:
    def apply(v: String): PuzzleId = v

  opaque type UserTitle <: String = String
  object UserTitle:
    def apply(v: String): UserTitle = v
