package lila.game

import lila.user.Context
// TODO
// import game.Query

// import com.mongodb.casbah.Imports.DBObject
// import scalaz.{ NonEmptyList, NonEmptyLists }

// sealed abstract class GameFilter(val name: String)

// object GameFilter {

//   case object All extends GameFilter("all")
//   case object Me extends GameFilter("me")
//   case object Rated extends GameFilter("rated")
//   case object Win extends GameFilter("win")
//   case object Loss extends GameFilter("loss")
//   case object Draw extends GameFilter("draw")
//   case object Playing extends GameFilter("playing")
//   case object Bookmark extends GameFilter("bookmark")
// }

// case class GameFilterMenu(
//     all: NonEmptyList[GameFilter],
//     current: GameFilter,
//     query: Option[DBObject],
//     cachedNb: Option[Int]) {

//   def list = all.list
// }

// object GameFilterMenu extends NonEmptyLists {

//   import GameFilter._

//   def apply(
//     info: UserInfo,
//     me: Option[User],
//     currentName: String): GameFilterMenu = {

//     val user = info.user

//     val all = nel(All, List(
//       info.nbWithMe.zmap(_ > 0) option Me,
//       (info.nbRated > 0) option Rated,
//       (info.user.nbWins > 0) option Win,
//       (info.user.nbLosses > 0) option Loss,
//       (info.user.nbDraws > 0) option Draw,
//       (info.nbPlaying > 0) option Playing,
//       (info.nbBookmark > 0) option Bookmark
//     ).flatten)

//     val current = (all.list find (_.name == currentName)) | all.head

//     val query: Option[DBObject] = current match {
//       case All      ⇒ Some(Query user user)
//       case Me       ⇒ Some(Query.opponents(user, me | user))
//       case Rated    ⇒ Some(Query rated user)
//       case Win      ⇒ Some(Query win user)
//       case Loss     ⇒ Some(Query loss user)
//       case Draw     ⇒ Some(Query draw user)
//       case Playing  ⇒ Some(Query notFinished user)
//       case Bookmark ⇒ None
//     }

//     val cachedNb: Option[Int] = current match {
//       case All   ⇒ info.user.nbGames.some
//       case Rated ⇒ info.nbRated.some
//       case Win   ⇒ info.user.nbWins.some
//       case Loss  ⇒ info.user.nbLosses.some
//       case Draw  ⇒ info.user.nbDraws.some
//       case _     ⇒ None
//     }

//     new GameFilterMenu(all, current, query, cachedNb)
//   }
// }
