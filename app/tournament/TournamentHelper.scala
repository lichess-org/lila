package lila
package tournament

import http.Context
import user.User

import com.codahale.jerkson.Json
import scala.math.{ min, max, round }

trait TournamentHelper { 

  def tournamentJsData(
    tour: Tournament, 
    version: Int,
    user: Option[User]) = Json generate {

    Map(
      "tournament" -> Map(
        "id" -> tour.id
      ),
      "version" -> version
    ).combine(user) { (map, u) => 
      map + ("username" -> u.username)
    }
  }
}
