package lila.gameSearch

import play.api.data.FormBinding
import play.api.mvc.Request
import play.api.i18n.Lang

import lila.game.Game

final class UserGameSearch(
    forms: GameSearchForm,
    paginator: lila.search.PaginatorBuilder[Game, Query]
):

  def apply(user: lila.user.User, page: Int)(using Request[?], FormBinding, Lang) =
    paginator(
      query = forms.search
        .bindFromRequest()
        .fold(
          _ => SearchData(SearchPlayer(a = (user.id into UserStr).some)),
          data =>
            data.copy(
              players = data.players.copy(a = (user.id into UserStr).some)
            )
        )
        .query,
      page = page
    )

  def requestForm(using Request[?], FormBinding, Lang) =
    forms.search.bindFromRequest()

  def defaultForm(using Lang) = forms.search
