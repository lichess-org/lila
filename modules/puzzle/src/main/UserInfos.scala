package lila.puzzle

import play.api.libs.json._
import reactivemongo.bson._

import lila.db.dsl._
import lila.rating.Glicko
import lila.user.User

case class UserInfos(user: User, history: List[Round], chart: JsArray)

object UserInfos {

  private def historySize = 20
  private def chartSize = 12

  import Round.roundBSONHandler

  lazy val defaultChart = JsArray {
    List.fill(chartSize)(Glicko.default.intRating) map { JsNumber(_) }
  }

  def apply(roundColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchRounds(user.id) map { rounds =>
      new UserInfos(user, makeHistory(rounds), makeChart(rounds))
    } recover {
      case e: Exception =>
        logger.error("user infos", e)
        new UserInfos(user, Nil, JsArray())
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchRounds(userId: String): Fu[List[Round]] =
      roundColl.find(BSONDocument(
        Round.BSONFields.userId -> userId
      )).sort(BSONDocument(
        Round.BSONFields.date -> -1
      )).cursor[Round]()
        .gather[List](math.max(historySize, chartSize))
  }

  private def makeHistory(rounds: List[Round]) = rounds.take(historySize)

  private def makeChart(rounds: List[Round]) = JsArray {
    val ratings = rounds.take(chartSize).reverse map (_.userPostRating)
    val filled = List.fill(chartSize - ratings.size)(Glicko.default.intRating) ::: ratings
    filled map { JsNumber(_) }
  }
}
