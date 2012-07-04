package lila
package timeline

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

import core.Settings

final class TimelineEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    lobbyNotify: Entry ⇒ IO[Unit],
    getUsername: String ⇒ String) {

  import settings._

  lazy val entryRepo = new EntryRepo(
    collection = mongodb(settings.LobbyCollectionEntry),
    max = LobbyEntryMax)

  lazy val push = new Push(
    entryRepo = entryRepo,
    lobbyNotify = lobbyNotify,
    getUsername = getUsername)
}
