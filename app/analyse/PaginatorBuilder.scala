package lila
package analyse

import game.{ DbGame, GameRepo }

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

final class PaginatorBuilder(
    analysisRepo: AnalysisRepo,
    cached: Cached,
    gameRepo: GameRepo,
    maxPerPage: Int) {

  def games(page: Int): Paginator[DbGame] = {
    paginator(GameAdapter, page)
  }

  private def paginator(adapter: Adapter[DbGame], page: Int): Paginator[DbGame] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    ).fold(_ ⇒ paginator(adapter, 0), identity)

  private object GameAdapter extends Adapter[DbGame] {

    def nbResults: Int = cached.nbAnalysis

    def slice(offset: Int, length: Int): Seq[DbGame] = {
      val ids = ((analysisRepo.collection.find(query, select) sort sort skip offset limit length).toList map {
        _.getAs[String]("_id")
      }).flatten
      val games = (gameRepo games ids).unsafePerformIO
      ids map { id ⇒ games find (_.id == id) }
    } flatten

    private def query = DBObject("done" -> true)
    private def select = DBObject("_id" -> true)
    private def sort = DBObject("date" -> -1)
  }
}
