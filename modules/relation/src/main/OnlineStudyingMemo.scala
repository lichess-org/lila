package lila.relation

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

final class OnlineStudyingMemo(ttl: Duration) {

	private val cache: Cache[String, String] /* userId, studyId */ = Scaffeine()
	  .expireAfterAccess(ttl)
	  .build[String, String]

	def put(userId: String, studyId: String): Unit = 
	  cache.put(userId, studyId)

	def get(userId: String): Option[String] =
	  cache getIfPresent userId

	def remove(userId: String): Unit =
	  cache invalidate userId
}