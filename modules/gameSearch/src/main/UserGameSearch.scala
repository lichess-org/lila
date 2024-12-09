package lila.gameSearch

import play.api.data.FormBinding
import play.api.i18n.Lang
import play.api.mvc.Request

import lila.search.spec.Query

final class UserGameSearch(
    forms: GameSearchForm,
    paginator: lila.search.PaginatorBuilder[Game, Query.Game]
)(using lila.core.i18n.Translator):

  def apply(user: User, page: Int)(using Request[?], FormBinding, Lang) =
    paginator(
      query = forms.search
        .bindFromRequest()
        .fold(
          _ => SearchData(SearchPlayer(a = (user.id.into(UserStr)).some)),
          data =>
            data.copy(
              players = data.players.copy(a = (user.id.into(UserStr)).some)
            )
        )
        .query,
      page = page
    )

  def requestForm(using Request[?], FormBinding, Lang) =
    forms.search.bindFromRequest()

  def defaultForm(using Lang) = forms.search
