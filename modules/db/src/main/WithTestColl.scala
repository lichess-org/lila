package lila.db

import scala.concurrent._
import scala.concurrent.duration._

import org.specs2.execute.{ Result, AsResult }
import reactivemongo.api._
import Types._

trait WithColl {

  def withColl[A](f: Coll => A): A = {

    implicit val ec = ExecutionContext.Implicits.global

    val conn = new MongoDriver connection List("localhost:27017")
    val db = conn("lila-test") 
    db.drop.await
    val coll = db("test_" + ornicar.scalalib.Random.nextString(8))
    val res = f(coll)
    conn.close
    res
  }
}
