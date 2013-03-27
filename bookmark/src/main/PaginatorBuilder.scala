package lila.bookmark

import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }
import lila.common.paginator._
import lila.common.PimpedJson._
import lila.db.paginator._
import lila.db.Implicits._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._
import org.joda.time.DateTime

final class PaginatorBuilder(
    bookmarkRepo: BookmarkRepo,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    maxPerPage: Int) extends lila.db.api.Full {

  def byUser(user: User, page: Int): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = count(selector)(bookmarkRepo.coll)

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = for {
      pairs ← query(selector)(bookmarkRepo.coll)
        .sort(sorting)
        .skip(offset)
        .limit(length)
        .cursor[JsObject].toList map2 { (obj: JsObject) ⇒
          obj.get[String]("g") flatMap { gameId ⇒
            obj.get[DateTime]("d") map { (gameId, _) }
          }
        } map (_.flatten)
      games ← gameRepo.find byIds pairs.map(_._1)
      bookmarks = pairs map { pair ⇒
        games find (_.id == pair._1) map { game ⇒
          Bookmark(game, user, pair._2)
        }
      }
    } yield bookmarks.toList.flatten

    private def selector = bookmarkRepo userIdQuery user.id
    private def sorting = sort desc "d"
  }
}
