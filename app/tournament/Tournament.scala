package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key

case class Tournament(
    @Key("_id") id: String,
    createdBy: String,
    maxUsers: Int,
    createdAt: DateTime = DateTime.now,
    status: Status = Status.Created,
    users: List[String]) {

}

object Tournament {

}
