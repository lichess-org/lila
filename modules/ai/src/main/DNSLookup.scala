package lila.ai

import javax.naming.directory.{ InitialDirContext, Attribute }
import javax.naming.NamingException
import scala.collection.JavaConversions._
import scala.concurrent.Future

import lila.memo.AsyncCache

private[ai] object DNSLookup {

  private val cache = AsyncCache(lookupInFuture)

  def apply(url: String): Fu[AiHost] = cache(url)

  private def lookupInFuture(url: String): Fu[AiHost] = Future {
    val host = new java.net.URL(url).getHost
    lookup(host).headOption map { AiHost(host, _) }
  } flatten s"Can't lookup $url IP address" recover {
    case e: Exception ⇒ {
      play.api.Logger("ai").warn(e.getMessage)
      AiHost("dns-fail", "127.0.0.1")
    }
  }

  // based on http://ujihisa.blogspot.fr/2012/09/dns-lookup-in-scala.html
  private def lookup(host: String): List[String] = {
    val attributes = try {
      new InitialDirContext getAttributes ("dns:/%s" format host)
    }
    catch {
      case _: NamingException ⇒ return Nil
    }
    val list = {
      val attributeEnumeration = attributes.getAll
      var list = List[Attribute]()
      while (attributeEnumeration.hasMore)
        list = attributeEnumeration.next :: list
      attributeEnumeration.close
      list.reverse
    }
    list map (x ⇒ x.getID -> x.get.toString) flatMap {
      case ("A", x)     ⇒ List(x)
      case ("CNAME", x) ⇒ lookup(x)
      case (_, x)       ⇒ Nil
    }
  }
}
