package lila.db
package test

import play.api.test._

abstract class WithDb extends WithApplication(WithDb.fakeApp)

object WithDb {

  val fakeApp = FakeApplication(
    additionalPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin")
  )

}
