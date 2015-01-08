package lila.opening

import reactivemongo.bson._
import reactivemongo.bson.Macros
import play.api.libs.json._

import lila.db.Types.Coll
import lila.rating.Glicko
import lila.user.User

case class UserInfos(user: User, history: List[Attempt], chart: JsArray) {

  def score = if (history.isEmpty) 50f
  else history.foldLeft(0)(_ + _.score) / history.size
}

object UserInfos {

  private def historySize = 20
  private def chartSize = 12

  import Attempt.attemptBSONHandler

  def apply(attemptColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchAttempts(user.id) map { attempts =>
      new UserInfos(user, makeHistory(attempts), makeChart(attempts))
    } recover {
      case e: Exception =>
        play.api.Logger("Opening UserInfos").error(e.getMessage)
        new UserInfos(user, Nil, JsArray())
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchAttempts(userId: String): Fu[List[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.userId -> userId
      )).sort(BSONDocument(
        Attempt.BSONFields.date -> -1
      )).cursor[Attempt].collect[List](math.max(historySize, chartSize))
  }

  private def makeHistory(attempts: List[Attempt]) = attempts.take(historySize)

  private def makeChart(attempts: List[Attempt]) = JsArray {
    val scores = attempts.take(chartSize).reverse map (_.score)
    val filled = List.fill(chartSize - scores.size)(0) ::: scores
    filled map { JsNumber(_) }
  }
}
