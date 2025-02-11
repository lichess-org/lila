package lila.prismic

trait DocumentLinkResolver {
  def apply(link: Fragment.DocumentLink): String
  def apply(document: Document): String = apply(
    Fragment.DocumentLink(
      document.id,
      document.uid,
      document.typ,
      document.tags,
      document.slug,
      isBroken = false,
    ),
  )
}

object DocumentLinkResolver {

  def apply(f: Fragment.DocumentLink => String) = new DocumentLinkResolver {
    def apply(link: Fragment.DocumentLink): String = f(link)
  }

}
