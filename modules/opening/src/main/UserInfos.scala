package lila.opening

import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.Types.Coll
import lila.rating.Glicko
import lila.user.User

case class UserInfos(user: User, history: List[Attempt], chart: JsArray)

object UserInfos {

  private def historySize = 20
  private def chartSize = 12

  import Attempt.attemptBSONHandler

  def apply(attemptColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchAttempts(user.id) map { attempts =>
      new UserInfos(user, makeHistory(attempts), makeChart(attempts))
    } recover {
      case e: Exception =>
        lila.log("opening").error("user infos", e)
        new UserInfos(user, Nil, JsArray())
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchAttempts(userId: String): Fu[List[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.userId -> userId
      )).sort(BSONDocument(
        Attempt.BSONFields.date -> -1
      )).cursor[Attempt]().collect[List](math.max(historySize, chartSize))
  }

  private def makeHistory(attempts: List[Attempt]) = attempts.take(historySize)

  private def makeChart(attempts: List[Attempt]) = JsArray {
    val ratings = attempts.take(chartSize).reverse map (_.userPostRating)
    val filled = List.fill(chartSize - ratings.size)(Glicko.default.intRating) ::: ratings
    filled map { JsNumber(_) }
  }
}
