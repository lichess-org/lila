package lila.coordinate

import play.api.Configuration

final class Env(
    appConfig: Configuration,
    db: lila.db.Env
) {

  private val CollectionScore = appConfig.get[String]("coordinate.collection.score")

  lazy val api = new CoordinateApi(scoreColl = scoreColl)

  lazy val forms = DataForm

  private[coordinate] lazy val scoreColl = db(CollectionScore)
}
