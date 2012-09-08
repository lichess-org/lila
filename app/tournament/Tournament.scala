package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key

case class Tournament(
  @Key("_id") id: String,
  createdAt: DateTime,
  createdBy: String) {

}
