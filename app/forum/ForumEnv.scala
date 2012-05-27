package lila
package forum

import user.{ User, UserRepo }
import core.Settings
import site.Captcha

import com.mongodb.casbah.MongoCollection

final class ForumEnv(
    settings: Settings,
    captcha: Captcha,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo) {

  import settings._

  lazy val categRepo = new CategRepo(mongodb(MongoCollectionForumCateg))

  lazy val topicRepo = new TopicRepo(mongodb(MongoCollectionForumTopic))

  lazy val postRepo = new PostRepo(mongodb(MongoCollectionForumPost))

  lazy val categApi = new CategApi(this)

  lazy val topicApi = new TopicApi(this, ForumTopicMaxPerPage)

  lazy val postApi = new PostApi(this, ForumPostMaxPerPage)

  lazy val recent = new Recent(
    env = this,
    timeout = ForumRecentTimeout)
  
  lazy val forms = new DataForm(captcha)

  lazy val denormalize = topicApi.denormalize flatMap { _ ⇒ categApi.denormalize }
}
