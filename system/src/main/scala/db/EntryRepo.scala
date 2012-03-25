package lila.system
package db

import model.Entry

import com.mongodb.casbah.MongoCollection

class EntryRepo(collection: MongoCollection, val max: Int)
    extends TimelineRepo[Entry](collection, max)
