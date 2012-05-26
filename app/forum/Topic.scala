package lila
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key

case class Topic(
    @Key("_id") id: String,
    slug: String,
    categId: String,
    name: String,
    views: Int,
    createdAt: DateTime,
    updatedAt: DateTime,
    nbPosts: Int = 0,
    lastPostId: String = "") {
}
