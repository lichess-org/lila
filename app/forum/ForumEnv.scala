package lila
package forum

import user.UserRepo
import core.Settings

import com.mongodb.casbah.MongoCollection

final class ForumEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo) {

  import settings._

  lazy val categRepo = new CategRepo(mongodb(MongoCollectionForumCateg))

  lazy val topicRepo = new TopicRepo(mongodb(MongoCollectionForumTopic))

  lazy val postRepo = new PostRepo(mongodb(MongoCollectionForumPost))

  lazy val categApi = new CategApi(this)

  lazy val topicApi = new TopicApi(this)

  lazy val denormalize = topicApi.denormalize flatMap { _ ⇒ categApi.denormalize }
}
