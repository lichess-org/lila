package lila
package timeline

import com.mongodb.casbah.MongoCollection

final class TimelineEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val entryRepo = new EntryRepo(
    collection = mongodb(settings.MongoCollectionEntry),
    max = LobbyEntryMax)
}
