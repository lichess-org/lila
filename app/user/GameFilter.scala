package lila
package user

import http.Context
import game.Query

import com.mongodb.casbah.Imports.DBObject
import scalaz.{ NonEmptyList, NonEmptyLists }

sealed abstract class GameFilter(val name: String)

object GameFilter {

  case object All extends GameFilter("all")
  case object Me extends GameFilter("me")
  case object Rated extends GameFilter("rated")
  case object Win extends GameFilter("win")
  case object Loss extends GameFilter("loss")
  case object Draw extends GameFilter("draw")
  case object Playing extends GameFilter("playing")
}

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter,
    query: DBObject) {

  def list = all.list
}

object GameFilterMenu extends NonEmptyLists {

  import GameFilter._

  def apply(
    info: UserInfo,
    me: Option[User],
    currentName: String): GameFilterMenu = {

    val user = info.user

    val all = nel(All, List(
      (info.user.some != me) option Me,
      (info.nbRated > 0) option Rated,
      (info.nbWin > 0) option Win,
      (info.nbLoss > 0) option Loss,
      (info.nbDraw > 0) option Draw,
      (info.nbPlaying > 0) option Playing
    ).flatten)

    val current = (all.list find (_.name == currentName)) | all.head

    val query = current match {
      case All     ⇒ Query user user
      case Me      ⇒ Query.opponents(user, me | user)
      case Rated   ⇒ Query rated user
      case Win     ⇒ Query win user
      case Loss    ⇒ Query loss user
      case Draw    ⇒ Query draw user
      case Playing ⇒ Query notFinished user
    }

    new GameFilterMenu(all, current, query)
  }
}
