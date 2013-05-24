package lila.analyse

import play.api.libs.json.Json

import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.game.Game
import lila.game.tube.gameTube
import tube.analysisTube

private[analyse] final class PaginatorBuilder(
    cached: { def nbAnalysis: Fu[Int] },
    maxPerPage: Int) {

  def games(page: Int): Fu[Paginator[Game]] = Paginator(
    adapter = GameAdapter,
    currentPage = page,
    maxPerPage = maxPerPage)

  private object GameAdapter extends AdapterLike[Game] {

    def nbResults = cached.nbAnalysis

    def slice(offset: Int, length: Int): Fu[Seq[Game]] = for {
      ids ← $primitive[Analysis, String](
        selector, 
        "_id", 
        _ sort sorting skip offset, 
        length.some)(_.asOpt[String])
      games ← $find.byOrderedIds[Game](ids)
    } yield games

    private def selector = Json.obj("done" -> true)
    private def sorting = $sort desc "date"
  }
}
