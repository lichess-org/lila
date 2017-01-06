package lila.search

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import play.api.data._
import play.api.data.Forms._

import lila.common.LightUser
import lila.common.paginator._
import lila.common.PimpedJson._
import lila.game.Game

final class JsonView() {

  def showResults(pager: Paginator[Game]): Result =
    Ok(PaginatorJson(pager.mapResults { g =>
      Json.obj(
        "id" -> g.id
      )
    })
    )
}
