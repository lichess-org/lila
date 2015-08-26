package lila.gameSearch

import lila.common.paginator._
import lila.game.Game
import play.api.mvc.Request

final class UserGameSearch(
    forms: DataForm,
    paginator: lila.search.PaginatorBuilder[Game],
    indexType: String) {

  def apply(user: lila.user.User, page: Int)(implicit req: Request[_]): Fu[Paginator[Game]] =
    paginator(
      query = forms.search.bindFromRequest.fold(
        _ => SearchData(SearchPlayer(a = user.id.some)),
        data => data.copy(
          players = data.players.copy(a = user.id.some)
        )
      ) query indexType,
      page = page)

  def requestForm(implicit req: Request[_]) = forms.search.bindFromRequest

  def defaultForm = forms.search
}
