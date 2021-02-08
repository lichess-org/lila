package lila.gameSearch

import play.api.data.FormBinding
import play.api.mvc.Request

import lila.game.Game

final class UserGameSearch(
    forms: GameSearchForm,
    paginator: lila.search.PaginatorBuilder[Game, Query]
) {

  def apply(user: lila.user.User, page: Int)(implicit req: Request[_], formBinding: FormBinding) =
    paginator(
      query = forms.search
        .bindFromRequest()
        .fold(
          _ => SearchData(SearchPlayer(a = user.id.some)),
          data =>
            data.copy(
              players = data.players.copy(a = user.id.some)
            )
        )
        .query,
      page = page
    )

  def requestForm(implicit req: Request[_], formBinding: FormBinding) = forms.search.bindFromRequest()

  def defaultForm = forms.search
}
