package lila
package tournament

import http.Context

import com.codahale.jerkson.Json
import scala.math.{ min, max, round }

trait TournamentHelper { 

  def tournamentJsData(tour: Tournament, version: Int) = Json generate {

    Map(
      "tournament" -> Map(
        "id" -> tour.id
      ),
      "version" -> version
    )
  }
}
