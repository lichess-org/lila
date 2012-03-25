package lila.system
package db

import model.Message

import com.mongodb.casbah.MongoCollection

class MessageRepo(collection: MongoCollection, val max: Int)
    extends TimelineRepo[Message](collection, max)
