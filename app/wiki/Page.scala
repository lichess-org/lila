package lila
package wiki

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

case class Page(
    @Key("_id") id: String,
    title: String,
    body: String) {

      def slug = id
}
