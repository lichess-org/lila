package lila
package team

import core.Settings
import site.Captcha

import com.mongodb.casbah.MongoCollection

final class TeamEnv(
    settings: Settings,
    captcha: Captcha,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val repo = new TeamRepo(mongodb(TeamCollectionTeam))

  lazy val api = new TeamApi(
    repo = repo,
    maxPerPage = TeamPaginatorMaxPerPage)

  lazy val forms = new DataForm(captcha)
}
