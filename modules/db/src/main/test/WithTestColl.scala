package lila.db
package test

import Types._

import reactivemongo.api._
import scala.concurrent._
import scala.concurrent.duration._

import org.specs2.execute.{ Result, AsResult }

trait WithColl {

  def withColl[A](f: Coll ⇒ A): A = {

    implicit val ec = ExecutionContext.Implicits.global

    val timeout = 1 seconds

    val conn = new MongoDriver connection List("localhost:27017")
    val db = conn("lila-test") ~ { _.drop }
    val coll = db("test")
    val res = f(coll)
    conn.close
    res
  }
}
