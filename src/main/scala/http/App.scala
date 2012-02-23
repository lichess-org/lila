package lila
package http

import com.typesafe.play.mini._
import play.api.mvc._
import play.api.mvc.Results._

/**
* this application is registered via Global
*/
object App extends Application {
  def route = {
    case GET(Path("/foo")) & QueryString(qs) => Action{ request=>
      val result = QueryString(qs,"foo").getOrElse("noh")
      Ok(<h1>It works!, query String {result}</h1>).as("text/html")
    }
  }
}
