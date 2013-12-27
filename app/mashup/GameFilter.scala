package lila.app
package mashup

import lila.user.User
import lila.game.{ Game, Query }

import play.api.libs.json._
import scalaz.NonEmptyList

sealed abstract class GameFilter(val name: String)

object GameFilter {

  case object All extends GameFilter("all")
  case object Me extends GameFilter("me")
  case object Rated extends GameFilter("rated")
  case object Win extends GameFilter("win")
  case object Loss extends GameFilter("loss")
  case object Draw extends GameFilter("draw")
  case object Playing extends GameFilter("playing")
  case object Bookmark extends GameFilter("bookmark")
}

case class GameFilterMenu(
    all: NonEmptyList[GameFilter],
    current: GameFilter,
    query: Option[JsObject],
    cachedNb: Option[Int]) {

  def list = all.list
}

object GameFilterMenu {

  import GameFilter._
  import lila.db.Implicits.docId

  def apply(
    info: UserInfo,
    me: Option[User],
    currentName: String): GameFilterMenu = {

    val user = info.user

    val all = NonEmptyList.nel(All, List(
      (info.nbWithMe > 0) option Me,
      (info.nbRated > 0) option Rated,
      (info.user.count.win > 0) option Win,
      (info.user.count.loss > 0) option Loss,
      (info.user.count.draw > 0) option Draw,
      (info.nbPlaying > 0) option Playing,
      (info.nbBookmark > 0) option Bookmark
    ).flatten)

    val current = (all.list find (_.name == currentName)) | all.head

    val query: Option[JsObject] = current match {
      case All      ⇒ Some(Query started user)
      case Me       ⇒ Some(Query.opponents(user, me | user))
      case Rated    ⇒ Some(Query rated user)
      case Win      ⇒ Some(Query win user)
      case Loss     ⇒ Some(Query loss user)
      case Draw     ⇒ Some(Query draw user)
      case Playing  ⇒ Some(Query notFinished user)
      case Bookmark ⇒ None
    }

    val cachedNb: Option[Int] = current match {
      case All   ⇒ info.user.count.game.some
      case Rated ⇒ info.user.count.rated.some
      case Win   ⇒ info.user.count.win.some
      case Loss  ⇒ info.user.count.loss.some
      case Draw  ⇒ info.user.count.draw.some
      case _     ⇒ None
    }

    new GameFilterMenu(all, current, query, cachedNb)
  }
}
