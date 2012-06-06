package lila
package star

import game.{ DbGame, GameRepo }
import user.{ User, UserRepo }

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

final class PaginatorBuilder(
    starRepo: StarRepo,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    maxPerPage: Int) {

  def byUser(user: User, page: Int): Paginator[Star] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: Adapter[Star], page: Int): Paginator[Star] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    ).fold(_ ⇒ paginator(adapter, 0), identity)

  final class UserAdapter(user: User) extends Adapter[Star] {

    def nbResults: Int = starRepo.collection count query toInt

    def slice(offset: Int, length: Int): Seq[Star] = {
      val objs = ((starRepo.collection find query sort sort skip offset limit length).toList map { obj ⇒
        for {
          gameId ← obj.getAs[String]("g")
          date ← obj.getAs[DateTime]("d")
        } yield gameId -> date
      }).flatten
      val games = (gameRepo games objs.map(_._1)).unsafePerformIO
      objs map { obj ⇒
        games find (_.id == obj._1) map { game ⇒
          Star(game, user, obj._2)
        }
      }
    } flatten

    private def query = starRepo userIdQuery user.id
    private def sort = starRepo sortQuery -1
  }
}
