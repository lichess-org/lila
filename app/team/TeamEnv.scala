package lila
package team

import core.Settings

import com.mongodb.casbah.MongoCollection

final class TeamEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val repo = new TeamRepo(mongodb(TeamCollectionTeam))

  lazy val api = new TeamApi(repo = repo)
}
