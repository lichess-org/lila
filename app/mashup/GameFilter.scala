package lila.app
package mashup

import lila.game.{ Game, Query }
import lila.user.User

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

  val all = NonEmptyList.nel(All, List(Me, Rated, Win, Loss, Draw, Playing, Bookmark))

  def apply(
    info: UserInfo,
    me: Option[User],
    currentName: String): GameFilterMenu = {

    val user = info.user

    val filters = NonEmptyList.nel(All, List(
      (info.nbWithMe > 0) option Me,
      (info.nbRated > 0) option Rated,
      (info.user.count.win > 0) option Win,
      (info.user.count.loss > 0) option Loss,
      (info.user.count.draw > 0) option Draw,
      (info.nbPlaying > 0) option Playing,
      (info.nbBookmark > 0) option Bookmark
    ).flatten)

    val current = currentOf(filters, currentName)

    val query = queryOf(current, user, me)

    val cachedNb = cachedNbOf(user, current)

    new GameFilterMenu(filters, current, query, cachedNb)
  }

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    (filters.list find (_.name == name)) | filters.head

  def queryOf(filter: GameFilter, user: User, me: Option[User]) = filter match {
    case All      => Some(Query started user)
    case Me       => Some(Query.opponents(user, me | user))
    case Rated    => Some(Query rated user)
    case Win      => Some(Query win user)
    case Loss     => Some(Query loss user)
    case Draw     => Some(Query draw user)
    case Playing  => Some(Query notFinished user)
    case Bookmark => None
  }

  def cachedNbOf(user: User, filter: GameFilter): Option[Int] = filter match {
    case All   => user.count.game.some
    case Rated => user.count.rated.some
    case Win   => user.count.win.some
    case Loss  => user.count.loss.some
    case Draw  => user.count.draw.some
    case _     => None
  }
}
