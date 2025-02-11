package lila.prismic

trait WithFragments {

  def fragments: Map[String, Fragment]

  private val IndexedKey = """^([^\[]+)(\[\d+\])?$""".r

  def get(field: String): Option[Fragment] = fragments.get(field).orElse(getAll(field).headOption)

  def getAll(field: String): Seq[Fragment] = fragments.collect {
    case (IndexedKey(key, _), fragment) if key == field => fragment
  }.toSeq

  def getLink(field: String): Option[Fragment.Link] = get(field).flatMap {
    case a: Fragment.WebLink      => Some(a)
    case a: Fragment.MediaLink    => Some(a)
    case a: Fragment.DocumentLink => Some(a)
    case _                        => None
  }

  def getImage(field: String): Option[Fragment.Image] = get(field).flatMap {
    case a: Fragment.Image => Some(a)
    case a: Fragment.StructuredText =>
      a.blocks
        .collectFirst { case b: Fragment.StructuredText.Block.Image => b.view }
        .map(v => Fragment.Image(v))
    case _ => None
  }

  def getAllImages(field: String): Seq[Fragment.Image] = getAll(field).flatMap {
    case a: Fragment.Image => Seq(a)
    case a: Fragment.StructuredText =>
      a.blocks
        .collect { case b: Fragment.StructuredText.Block.Image => b.view }
        .map(v => Fragment.Image(v))
    case _ => Nil
  }

  def getImage(field: String, view: String): Option[Fragment.Image.View] = get(field).flatMap {
    case a: Fragment.Image                            => a.getView(view)
    case _: Fragment.StructuredText if view == "main" => getImage(field).map(_.main)
    case _                                            => None
  }

  def getAllImages(field: String, view: String): Seq[Fragment.Image.View] = getAll(field).flatMap {
    case a: Fragment.Image                            => a.getView(view).toSeq
    case _: Fragment.StructuredText if view == "main" => getAllImages(field).map(_.main)
    case _                                            => Nil
  }

  def getStructuredText(field: String): Option[Fragment.StructuredText] = get(field).flatMap {
    case a: Fragment.StructuredText => Some(a)
    case _                          => None
  }

  def getHtml(field: String, linkResolver: DocumentLinkResolver): Option[String] =
    get(field).flatMap {
      case a: Fragment.StructuredText => Some(a.asHtml(linkResolver))
      case a: Fragment.Number         => Some(a.asHtml)
      case a: Fragment.Color          => Some(a.asHtml)
      case a: Fragment.Text           => Some(a.asHtml)
      case a: Fragment.Date           => Some(a.asHtml)
      case a: Fragment.Timestamp      => Some(a.asHtml)
      case a: Fragment.Embed          => Some(a.asHtml())
      case a: Fragment.Image          => Some(a.asHtml)
      case a: Fragment.WebLink        => Some(a.asHtml)
      case a: Fragment.MediaLink      => Some(a.asHtml)
      case a: Fragment.GeoPoint       => Some(a.asHtml)
      case a: Fragment.DocumentLink   => Some(a.asHtml(linkResolver))
      case a: Fragment.Group          => Some(a asHtml linkResolver)
    }

  def getText(field: String): Option[String] = get(field).flatMap {
    case a: Fragment.StructuredText =>
      Some(a.blocks.collect { case b: Fragment.StructuredText.Block.Text => b.text }.mkString("\n"))
        .filterNot(_.isEmpty)
    case a: Fragment.Number => Some(a.value.toString)
    case a: Fragment.Color  => Some(a.hex)
    case a: Fragment.Text   => Some(a.value).filterNot(_.isEmpty)
    case a: Fragment.Date   => Some(a.value.toString)
    case _                  => None
  }

  def getColor(field: String): Option[Fragment.Color] = get(field).flatMap {
    case c: Fragment.Color => Some(c)
    case _                 => None
  }

  def getNumber(field: String): Option[Fragment.Number] = get(field).flatMap {
    case n: Fragment.Number => Some(n)
    case _                  => None
  }

  def getDate(field: String): Option[Fragment.Date] = get(field).flatMap {
    case d: Fragment.Date => Some(d)
    case _                => None
  }

  def getDate(field: String, pattern: String): Option[String] = get(field).flatMap {
    case d: Fragment.Date => Some(d.asText(pattern))
    case _                => None
  }

  def getTimestamp(field: String): Option[Fragment.Timestamp] = get(field).flatMap {
    case t: Fragment.Timestamp => Some(t)
    case _                     => None
  }

  def getEmbed(field: String): Option[Fragment.Embed] = get(field).flatMap {
    case e: Fragment.Embed => Some(e)
    case _                 => None
  }

  def getGeoPoint(field: String): Option[Fragment.GeoPoint] = get(field).flatMap {
    case gp: Fragment.GeoPoint => Some(gp)
    case _                     => None
  }

  def getNumber(field: String, pattern: String): Option[String] =
    getNumber(field).map(_.asText(pattern))

  def getBoolean(field: String): Boolean = get(field)
    .flatMap {
      case a: Fragment.Text =>
        Option(a.value.toLowerCase).collect {
          case "yes"  => true
          case "true" => true
        }
      case _ => None
    }
    .getOrElse(false)

  def getGroup(field: String): Option[Fragment.Group] = get(field).flatMap {
    case g: Fragment.Group => Some(g)
    case _                 => None
  }

  def asHtml(linkResolver: DocumentLinkResolver): String = fragments.keys
    .map { field =>
      s"""<section data-field="$field">${getHtml(field, linkResolver).getOrElse("")}</section>"""
    }
    .mkString("\n")

}
