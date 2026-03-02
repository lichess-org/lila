package lila.fide

import monocle.syntax.all.*
import reactivemongo.api.bson.Macros.Annotations.Key
import chess.FideTC

import lila.core.fide.Federation.*
import play.api.i18n.Lang
import java.util.Locale

case class Federation(
    @Key("_id") id: Id,
    name: Name,
    nbPlayers: Int,
    standard: Stats,
    rapid: Stats,
    blitz: Stats,
    updatedAt: Instant
):
  def stats(tc: FideTC) = tc match
    case FideTC.standard => this.focus(_.standard)
    case FideTC.rapid => this.focus(_.rapid)
    case FideTC.blitz => this.focus(_.blitz)
  def slug = Federation.nameToSlug(name)

// https://ratings.fide.com/top_federations.phtml
// all=[];$('#federations_table').find('tbody tr').each(function(){all.push([$(this).find('img').attr('src').slice(5,8),$(this).find('a,strong').text().trim()])})
object Federation:

  def nameToSlug(name: Name) = FidePlayer.slugify(chess.PlayerName(name))

  def idToSlug(id: Id): String = nameToSlug(name(id))

  def name(id: Id): Name = names.get(id).map(_._1).getOrElse(id.value)

  def i18nName(id: Id)(using lang: Lang): Name =
    val fed = names.get(id)
    fed
      .flatMap(_._2)
      .map: code =>
        val locale = Locale.of(lang.language, code)
        locale.getDisplayCountry(locale)
      .orElse(fed.map(_._1))
      .getOrElse(id.value)

  def find(str: String): Option[Id] =
    Id(str.toUpperCase).some
      .filter(names.contains)
      .orElse(bySlug.get(str))
      .orElse(bySlug.get(nameToSlug(str)))

  lazy val bySlug: Map[String, Id] =
    names.map: (id, name) =>
      nameToSlug(name._1) -> id

  // FIDE follows IOC country codes with some exceptions
  // https://en.wikipedia.org/wiki/List_of_IOC_country_codes
  // We also need ISO 3166-1 alpha-2 codes to get automatic i18n from Java
  // Feds without an Alpha2 code: England, Scotland, Wales, Kosovo, FIDE,
  // Netherlands Antilles (defunct in IOC but still in FIDE)
  // Taiwan (participates in IOC events as "Chinese Taipei")
  type Alpha2 = String
  val names: Map[Id, (String, Option[Alpha2])] = Map(
    Id("AFG") -> ("Afghanistan", Some("AF")),
    Id("AHO") -> ("Netherlands Antilles", None),
    Id("ALB") -> ("Albania", Some("AL")),
    Id("ALG") -> ("Algeria", Some("DZ")),
    Id("AND") -> ("Andorra", Some("AD")),
    Id("ANG") -> ("Angola", Some("AO")),
    Id("ANT") -> ("Antigua and Barbuda", Some("AG")),
    Id("ARG") -> ("Argentina", Some("AR")),
    Id("ARM") -> ("Armenia", Some("AM")),
    Id("ARU") -> ("Aruba", Some("AW")),
    Id("AUS") -> ("Australia", Some("AU")),
    Id("AUT") -> ("Austria", Some("AT")),
    Id("AZE") -> ("Azerbaijan", Some("AZ")),
    Id("BAH") -> ("Bahamas", Some("BS")),
    Id("BAN") -> ("Bangladesh", Some("BD")),
    Id("BAR") -> ("Barbados", Some("BB")),
    Id("BDI") -> ("Burundi", Some("BI")),
    Id("BEL") -> ("Belgium", Some("BE")),
    Id("BER") -> ("Bermuda", Some("BM")),
    Id("BHU") -> ("Bhutan", Some("BT")),
    Id("BIH") -> ("Bosnia & Herzegovina", Some("BA")),
    Id("BIZ") -> ("Belize", Some("BZ")),
    Id("BLR") -> ("Belarus", Some("BY")),
    Id("BOL") -> ("Bolivia", Some("BO")),
    Id("BOT") -> ("Botswana", Some("BW")),
    Id("BRA") -> ("Brazil", Some("BR")),
    Id("BRN") -> ("Bahrain", Some("BN")),
    Id("BRU") -> ("Brunei Darussalam", Some("BN")),
    Id("BUL") -> ("Bulgaria", Some("BG")),
    Id("BUR") -> ("Burkina Faso", Some("BF")),
    Id("CAF") -> ("Central African Republic", Some("CF")),
    Id("CAM") -> ("Cambodia", Some("KH")),
    Id("CAN") -> ("Canada", Some("CA")),
    Id("CAY") -> ("Cayman Islands", Some("KY")),
    Id("CGO") -> ("Congo", Some("CG")),
    Id("CHA") -> ("Chad", Some("TD")),
    Id("CHI") -> ("Chile", Some("CL")),
    Id("CHN") -> ("China", Some("CN")),
    Id("CIV") -> ("Cote d’Ivoire", Some("CI")),
    Id("CMR") -> ("Cameroon", Some("CM")),
    Id("COD") -> ("Democratic Republic of the Congo", Some("CD")),
    Id("COL") -> ("Colombia", Some("CO")),
    Id("COM") -> ("Comoros Islands", Some("KM")),
    Id("CPV") -> ("Cape Verde", Some("CV")),
    Id("CRC") -> ("Costa Rica", Some("CR")),
    Id("CRO") -> ("Croatia", Some("HR")),
    Id("CUB") -> ("Cuba", Some("CU")),
    Id("CYP") -> ("Cyprus", Some("CY")),
    Id("CZE") -> ("Czech Republic", Some("CZ")),
    Id("DEN") -> ("Denmark", Some("DK")),
    Id("DJI") -> ("Djibouti", Some("DJ")),
    Id("DMA") -> ("Dominica", Some("DM")),
    Id("DOM") -> ("Dominican Republic", Some("DO")),
    Id("ECU") -> ("Ecuador", Some("EC")),
    Id("EGY") -> ("Egypt", Some("EG")),
    Id("ENG") -> ("England", None),
    Id("ERI") -> ("Eritrea", Some("ER")),
    Id("ESA") -> ("El Salvador", Some("SV")),
    Id("ESP") -> ("Spain", Some("ES")),
    Id("EST") -> ("Estonia", Some("EE")),
    Id("ETH") -> ("Ethiopia", Some("ET")),
    Id("FAI") -> ("Faroe Islands", Some("FO")),
    Id("FID") -> ("FIDE", None),
    Id("FIJ") -> ("Fiji", Some("FJ")),
    Id("FIN") -> ("Finland", Some("FI")),
    Id("FRA") -> ("France", Some("FR")),
    Id("GAB") -> ("Gabon", Some("GA")),
    Id("GAM") -> ("Gambia", Some("GM")),
    Id("GCI") -> ("Guernsey", Some("GG")),
    Id("GEO") -> ("Georgia", Some("GE")),
    Id("GEQ") -> ("Equatorial Guinea", Some("GQ")),
    Id("GER") -> ("Germany", Some("DE")),
    Id("GHA") -> ("Ghana", Some("GH")),
    Id("GRE") -> ("Greece", Some("GR")),
    Id("GRL") -> ("Greenland", Some("GL")),
    Id("GRN") -> ("Grenada", Some("GD")),
    Id("GUA") -> ("Guatemala", Some("GT")),
    Id("GUM") -> ("Guam", Some("GU")),
    Id("GUY") -> ("Guyana", Some("GY")),
    Id("HAI") -> ("Haiti", Some("HT")),
    Id("HKG") -> ("Hong Kong, China", Some("HK")),
    Id("HON") -> ("Honduras", Some("HN")),
    Id("HUN") -> ("Hungary", Some("HU")),
    Id("INA") -> ("Indonesia", Some("ID")),
    Id("IND") -> ("India", Some("IN")),
    Id("IOM") -> ("Isle of Man", Some("IM")),
    Id("IRI") -> ("Iran", Some("IR")),
    Id("IRL") -> ("Ireland", Some("IE")),
    Id("IRQ") -> ("Iraq", Some("IQ")),
    Id("ISL") -> ("Iceland", Some("IS")),
    Id("ISR") -> ("Israel", Some("IL")),
    Id("ISV") -> ("US Virgin Islands", Some("VI")),
    Id("ITA") -> ("Italy", Some("IT")),
    Id("IVB") -> ("British Virgin Islands", Some("VG")),
    Id("JAM") -> ("Jamaica", Some("JM")),
    Id("JCI") -> ("Jersey", Some("JE")),
    Id("JOR") -> ("Jordan", Some("JO")),
    Id("JPN") -> ("Japan", Some("JP")),
    Id("KAZ") -> ("Kazakhstan", Some("KZ")),
    Id("KEN") -> ("Kenya", Some("KE")),
    Id("KGZ") -> ("Kyrgyzstan", Some("KG")),
    Id("KOR") -> ("South Korea", Some("KR")),
    Id("KOS") -> ("Kosovo *", None),
    Id("KSA") -> ("Saudi Arabia", Some("SA")),
    Id("KUW") -> ("Kuwait", Some("KW")),
    Id("LAO") -> ("Laos", Some("LA")),
    Id("LAT") -> ("Latvia", Some("LV")),
    Id("LBA") -> ("Libya", Some("LY")),
    Id("LBN") -> ("Lebanon", Some("LB")),
    Id("LBR") -> ("Liberia", Some("LR")),
    Id("LCA") -> ("Saint Lucia", Some("LC")),
    Id("LES") -> ("Lesotho", Some("LS")),
    Id("LIE") -> ("Liechtenstein", Some("LI")),
    Id("LTU") -> ("Lithuania", Some("LT")),
    Id("LUX") -> ("Luxembourg", Some("LU")),
    Id("MAC") -> ("Macau", Some("MO")),
    Id("MAD") -> ("Madagascar", Some("MG")),
    Id("MAR") -> ("Morocco", Some("MA")),
    Id("MAS") -> ("Malaysia", Some("MY")),
    Id("MAW") -> ("Malawi", Some("MW")),
    Id("MDA") -> ("Moldova", Some("MD")),
    Id("MDV") -> ("Maldives", Some("MV")),
    Id("MEX") -> ("Mexico", Some("MX")),
    Id("MGL") -> ("Mongolia", Some("MN")),
    Id("MKD") -> ("North Macedonia", Some("MK")),
    Id("MLI") -> ("Mali", Some("ML")),
    Id("MLT") -> ("Malta", Some("MT")),
    Id("MNC") -> ("Monaco", Some("MC")),
    Id("MNE") -> ("Montenegro", Some("ME")),
    Id("MOZ") -> ("Mozambique", Some("MZ")),
    Id("MRI") -> ("Mauritius", Some("MU")),
    Id("MTN") -> ("Mauritania", Some("MR")),
    Id("MYA") -> ("Myanmar", Some("MM")),
    Id("NAM") -> ("Namibia", Some("NA")),
    Id("NCA") -> ("Nicaragua", Some("NI")),
    Id("NCL") -> ("New Caledonia", Some("NC")),
    Id("NED") -> ("Netherlands", Some("NL")),
    Id("NEP") -> ("Nepal", Some("NP")),
    Id("NGR") -> ("Nigeria", Some("NG")),
    Id("NIG") -> ("Niger", Some("NE")),
    Id("NOR") -> ("Norway", Some("NO")),
    Id("NRU") -> ("Nauru", Some("NR")),
    Id("NZL") -> ("New Zealand", Some("NZ")),
    Id("OMA") -> ("Oman", Some("OM")),
    Id("PAK") -> ("Pakistan", Some("PK")),
    Id("PAN") -> ("Panama", Some("PA")),
    Id("PAR") -> ("Paraguay", Some("PY")),
    Id("PER") -> ("Peru", Some("PE")),
    Id("PHI") -> ("Philippines", Some("PH")),
    Id("PLE") -> ("Palestine", Some("PS")),
    Id("PLW") -> ("Palau", Some("PW")),
    Id("PNG") -> ("Papua New Guinea", Some("PG")),
    Id("POL") -> ("Poland", Some("PL")),
    Id("POR") -> ("Portugal", Some("PT")),
    Id("PUR") -> ("Puerto Rico", Some("PR")),
    Id("QAT") -> ("Qatar", Some("QA")),
    Id("ROU") -> ("Romania", Some("RO")),
    Id("RSA") -> ("South Africa", Some("ZA")),
    Id("RUS") -> ("Russia", Some("RU")),
    Id("RWA") -> ("Rwanda", Some("RW")),
    Id("SCO") -> ("Scotland", None),
    Id("SEN") -> ("Senegal", Some("SN")),
    Id("SEY") -> ("Seychelles", Some("SC")),
    Id("SGP") -> ("Singapore", Some("SG")),
    Id("SKN") -> ("Saint Kitts and Nevis", Some("KN")),
    Id("SLE") -> ("Sierra Leone", Some("SL")),
    Id("SLO") -> ("Slovenia", Some("SI")),
    Id("SMR") -> ("San Marino", Some("SM")),
    Id("SOL") -> ("Solomon Islands", Some("SB")),
    Id("SOM") -> ("Somalia", Some("SO")),
    Id("SRB") -> ("Serbia", Some("RS")),
    Id("SRI") -> ("Sri Lanka", Some("LK")),
    Id("SSD") -> ("South Sudan", Some("SS")),
    Id("STP") -> ("Sao Tome and Principe", Some("ST")),
    Id("SUD") -> ("Sudan", Some("SD")),
    Id("SUI") -> ("Switzerland", Some("CH")),
    Id("SUR") -> ("Suriname", Some("SR")),
    Id("SVK") -> ("Slovakia", Some("SK")),
    Id("SWE") -> ("Sweden", Some("SE")),
    Id("SWZ") -> ("Eswatini", Some("SZ")),
    Id("SYR") -> ("Syria", Some("SY")),
    Id("TAN") -> ("Tanzania", Some("TZ")),
    Id("TGA") -> ("Tonga", Some("TO")),
    Id("THA") -> ("Thailand", Some("TH")),
    Id("TJK") -> ("Tajikistan", Some("TJ")),
    Id("TKM") -> ("Turkmenistan", Some("TM")),
    Id("TLS") -> ("Timor-Leste", Some("TL")),
    Id("TOG") -> ("Togo", Some("TG")),
    Id("TPE") -> ("Chinese Taipei", None),
    Id("TTO") -> ("Trinidad and Tobago", Some("TT")),
    Id("TUN") -> ("Tunisia", Some("TN")),
    Id("TUR") -> ("Turkiye", Some("TR")),
    Id("UAE") -> ("United Arab Emirates", Some("AE")),
    Id("UGA") -> ("Uganda", Some("UG")),
    Id("UKR") -> ("Ukraine", Some("UA")),
    Id("URU") -> ("Uruguay", Some("UY")),
    Id("USA") -> ("United States of America", Some("US")),
    Id("UZB") -> ("Uzbekistan", Some("UZ")),
    Id("VAN") -> ("Vanuatu", Some("VU")),
    Id("VEN") -> ("Venezuela", Some("VE")),
    Id("VIE") -> ("Vietnam", Some("VN")),
    Id("VIN") -> ("Saint Vincent and the Grenadines", Some("VC")),
    Id("WLS") -> ("Wales", None),
    Id("YEM") -> ("Yemen", Some("YE")),
    Id("ZAM") -> ("Zambia", Some("ZM")),
    Id("ZIM") -> ("Zimbabwe", Some("ZW"))
  )
