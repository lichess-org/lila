package lila.db
package test

import play.api.test._

abstract class WithDb extends WithApplication(WithDb.fakeApp)

object WithDb {

  lazy val fakeApp = FakeApplication(
    additionalPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin"),
    additionalConfiguration = Map(
      "mongodb.db" -> "lila-test",
      "application.Global" -> "play.api.DefaultGlobal"
    )
  )
}
