package lila.db
package test

import Types._

import play.api.Play
import play.api.test._
import reactivemongo.api._

import org.specs2.execute.{ Result, AsResult }

abstract class WithTestColl(a: FakeApplication = FakeApplication())
    extends WithApplication(a) {

  implicit lazy val conn = new MongoDriver connection List("localhost:27017")

  implicit lazy val coll: Coll = {
    val db = conn("test")
    val coll = db("test")
    coll.drop().await
    coll
  }

  override def around[T: AsResult](t: ⇒ T): Result = {
    running(app)(AsResult(t))
  }

  private def running[T](fakeApp: FakeApplication)(t: ⇒ T): T = synchronized {
    try {
      Play.start(fakeApp)
      t
    }
    finally {
      conn.close()
      Play.stop()
      play.api.libs.ws.WS.resetClient()
    }
  }
}
