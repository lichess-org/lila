package lila.base

trait LilaModel:

  opaque type GameId <: String = String
  object GameId:
    def apply(v: String): GameId = v

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

  opaque type StudyId <: String = String
  object StudyId:
    def apply(v: String): StudyId = v
