package lila.game

enum GameFilter:
  val name = lila.common.String.lcfirst(toString)
  case All, Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported, Search

object GameFilter:
  val all: NonEmptyList[GameFilter] =
    NonEmptyList.of(All, Me, Rated, Win, Loss, Draw, Playing, Bookmark, Imported, Search)

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter
):
  def list = all.toList
