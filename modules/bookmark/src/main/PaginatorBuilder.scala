package lila.bookmark

import lila.game.{ Game, GameRepo }
import lila.user.User
import lila.common.paginator._
import lila.common.PimpedJson._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import tube.bookmarkTube

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import org.joda.time.DateTime

final class PaginatorBuilder(maxPerPage: Int) {

  def byUser(user: User, page: Int): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = $count(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = for {
      pairs ← $query(selector)
        .sort(sorting)
        .skip(offset)
        .limit(length)
        .cursor[JsObject].toList map2 { (obj: JsObject) ⇒
          obj str "g" flatMap { gameId ⇒
            obj.get[DateTime]("d") map { (gameId, _) }
          }
        } map (_.flatten)
      games ← lila.game.tube.gameTube |> { implicit t ⇒
        $find.byIds[Game](pairs map (_._1))
      }
      bookmarks = pairs map { pair ⇒
        games find (_.id == pair._1) map { game ⇒
          Bookmark(game, user, pair._2)
        }
      }
    } yield bookmarks.toList.flatten

    private def selector = BookmarkRepo userIdQuery user.id
    private def sorting = $sort desc "d"
  }
}
