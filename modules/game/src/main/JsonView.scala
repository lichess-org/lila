package lila.game

import play.api.libs.json._

import chess.variant.Crazyhouse

object JsonView {

  implicit val crosstableResultWrites = Json.writes[Crosstable.Result]

  implicit val crosstableWrites = OWrites[Crosstable] { c =>
    Json.obj(
      "users" -> JsObject(c.users.map { u =>
        u.id -> JsNumber(u.score / 10d)
      }),
      "results" -> c.results,
      "nbGames" -> c.nbGames)
  }

  implicit val crazyhousePocketWriter: OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      })
  }

  implicit val crazyhouseDataWriter: OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }
}
