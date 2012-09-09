package lila
package tournament

import game.{ GameRepo, DbGame }
import core.Settings

import com.traackr.scalastic.elasticsearch
import com.mongodb.casbah.MongoCollection

final class TournamentEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val forms = new DataForm

  lazy val repo = new TournamentRepo(
    collection = mongodb(TournamentCollectionTournament))

  lazy val api = new TournamentApi(
    repo = repo)
}
