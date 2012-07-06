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

  lazy val categRepo = new CategRepo(mongodb(ForumCollectionCateg))

  lazy val topicRepo = new TopicRepo(mongodb(ForumCollectionTopic))

  lazy val postRepo = new PostRepo(mongodb(ForumCollectionPost))

  lazy val categApi = new CategApi(this)

  lazy val topicApi = new TopicApi(this, ForumTopicMaxPerPage)

  lazy val postApi = new PostApi(this, ForumPostMaxPerPage)

  lazy val recent = new Recent(
    env = this,
    timeout = ForumRecentTimeout)
  
  lazy val forms = new DataForm(captcha)

  lazy val denormalize = topicApi.denormalize flatMap { _ ⇒ categApi.denormalize }
}
