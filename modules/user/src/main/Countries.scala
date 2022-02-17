package lila.user

import scala._

final class Country(
    val code: String,
    val name: String,
    val shortName: String
)

object Countries {

  @inline private def C(code: String, name: String)                    = new Country(code, name, name)
  @inline private def C(code: String, name: String, shortName: String) = new Country(code, name, shortName)

  val all = List(
    C("AD", "Andorra"),
    C("AE", "United Arab Emirates", "UAE"),
    C("AF", "Afghanistan"),
    C("AG", "Antigua and Barbuda"),
    C("AI", "Anguilla"),
    C("AL", "Albania"),
    C("AM", "Armenia"),
    C("AO", "Angola"),
    C("AQ", "Antarctica"),
    C("AR", "Argentina"),
    C("AS", "American Samoa"),
    C("AT", "Austria"),
    C("AU", "Australia"),
    C("AW", "Aruba"),
    C("AX", "Aland Islands"),
    C("AZ", "Azerbaijan"),
    C("BA", "Bosnia-Herzegovina"),
    C("BB", "Barbados"),
    C("BD", "Bangladesh"),
    C("BE", "Belgium"),
    C("BF", "Burkina Faso"),
    C("BG", "Bulgaria"),
    C("BH", "Bahrain"),
    C("BI", "Burundi"),
    C("BJ", "Benin"),
    C("BL", "Saint Barthelemy"),
    C("BM", "Bermuda"),
    C("BN", "Brunei"),
    C("BO", "Bolivia"),
    C("BQ", "Bonaire, Sint Eustatius and Saba"),
    C("BR", "Brazil"),
    C("BS", "Bahamas"),
    C("BT", "Bhutan"),
    C("BV", "Bouvet Island"),
    C("BW", "Botswana"),
    C("BY", "Belarus"),
    C("BZ", "Belize"),
    C("CA", "Canada"),
    C("CA-QC", "Quebec"),
    C("CC", "Cocos (Keeling) Islands"),
    C("CD", "Congo (Democratic Rep.)"),
    C("CF", "Central African Republic"),
    C("CG", "Congo (Brazzaville)"),
    C("CH", "Switzerland"),
    C("CI", "Cote d'Ivoire"),
    C("CK", "Cook Islands"),
    C("CL", "Chile"),
    C("CM", "Cameroon"),
    C("CN", "China"),
    C("CO", "Colombia"),
    C("CR", "Costa Rica"),
    C("CU", "Cuba"),
    C("CV", "Cape Verde"),
    C("CW", "Curaçao"),
    C("CX", "Christmas Island"),
    C("CY", "Cyprus"),
    C("CZ", "Czechia"),
    C("DE", "Germany"),
    C("DJ", "Djibouti"),
    C("DK", "Denmark"),
    C("DM", "Dominica"),
    C("DO", "Dominican Republic"),
    C("DZ", "Algeria"),
    C("EC", "Ecuador"),
    C("EE", "Estonia"),
    C("EG", "Egypt"),
    C("EH", "Western Sahara"),
    C("ER", "Eritrea"),
    C("ES", "Spain"),
    C("ES-CT", "Catalonia"),
    C("ES-EU", "Basque Country"),
    C("ET", "Ethiopia"),
    C("FI", "Finland"),
    C("FJ", "Fiji"),
    C("FK", "Falkland Islands"),
    C("FM", "Micronesia"),
    C("FO", "Faroe Islands"),
    C("FR", "France"),
    C("GA", "Gabon"),
    C("GB", "United Kingdom", "UK"),
    C("GB-ENG", "England"),
    C("GB-NIR", "Northern Ireland"),
    C("GB-SCT", "Scotland"),
    C("GB-WLS", "Wales"),
    C("GD", "Grenada"),
    C("GE", "Georgia"),
    C("GF", "French Guiana"),
    C("GG", "Guernsey"),
    C("GH", "Ghana"),
    C("GI", "Gibraltar"),
    C("GL", "Greenland"),
    C("GM", "Gambia"),
    C("GN", "Guinea"),
    C("GP", "Guadeloupe"),
    C("GQ", "Equatorial Guinea"),
    C("GR", "Greece"),
    C("GS", "South Georgia and the South Sandwich Islands"),
    C("GT", "Guatemala"),
    C("GU", "Guam"),
    C("GW", "Guinea-Bissau"),
    C("GY", "Guyana"),
    C("HK", "Hong Kong"),
    C("HM", "Heard Island and McDonald Islands"),
    C("HN", "Honduras"),
    C("HR", "Croatia"),
    C("HT", "Haiti"),
    C("HU", "Hungary"),
    C("ID", "Indonesia"),
    C("IE", "Ireland"),
    C("IL", "Israel"),
    C("IM", "Isle of Man"),
    C("IN", "India"),
    C("IO", "British Indian Ocean Territory"),
    C("IQ", "Iraq"),
    C("IR", "Iran"),
    C("IS", "Iceland"),
    C("IT", "Italy"),
    C("JE", "Jersey"),
    C("JM", "Jamaica"),
    C("JO", "Jordan"),
    C("JP", "Japan"),
    C("KE", "Kenya"),
    C("KG", "Kyrgyzstan"),
    C("KH", "Cambodia"),
    C("KI", "Kiribati"),
    C("KM", "Comoros"),
    C("KN", "Saint Kitts and Nevis"),
    C("KP", "North Korea"),
    C("KR", "South Korea"),
    C("KW", "Kuwait"),
    C("KY", "Cayman Islands"),
    C("KZ", "Kazakhstan"),
    C("LA", "Laos"),
    C("LB", "Lebanon"),
    C("LC", "Saint Lucia"),
    C("LI", "Liechtenstein"),
    C("LK", "Sri Lanka"),
    C("LR", "Liberia"),
    C("LS", "Lesotho"),
    C("LT", "Lithuania"),
    C("LU", "Luxembourg"),
    C("LV", "Latvia"),
    C("LY", "Libya"),
    C("MA", "Morocco"),
    C("MC", "Monaco"),
    C("MD", "Moldova"),
    C("ME", "Montenegro"),
    C("MF", "Saint Martin"),
    C("MG", "Madagascar"),
    C("MH", "Marshall Islands"),
    C("MK", "North Macedonia"),
    C("ML", "Mali"),
    C("MM", "Myanmar"),
    C("MN", "Mongolia"),
    C("MO", "Macao"),
    C("MP", "Northern Mariana Islands"),
    C("MQ", "Martinique"),
    C("MR", "Mauritania"),
    C("MS", "Montserrat"),
    C("MT", "Malta"),
    C("MU", "Mauritius"),
    C("MV", "Maldives"),
    C("MW", "Malawi"),
    C("MX", "Mexico"),
    C("MY", "Malaysia"),
    C("MZ", "Mozambique"),
    C("NA", "Namibia"),
    C("NC", "New Caledonia"),
    C("NE", "Niger"),
    C("NF", "Norfolk Island"),
    C("NG", "Nigeria"),
    C("NI", "Nicaragua"),
    C("NL", "Netherlands"),
    C("NO", "Norway"),
    C("NP", "Nepal"),
    C("NR", "Nauru"),
    C("NU", "Niue"),
    C("NZ", "New Zealand"),
    C("OM", "Oman"),
    C("PA", "Panama"),
    C("PE", "Peru"),
    C("PF", "French Polynesia"),
    C("PG", "Papua New Guinea"),
    C("PH", "Philippines"),
    C("PK", "Pakistan"),
    C("PL", "Poland"),
    C("PM", "Saint Pierre and Miquelon"),
    C("PN", "Pitcairn"),
    C("PR", "Puerto Rico"),
    C("PS", "Palestine"),
    C("PT", "Portugal"),
    C("PW", "Palau"),
    C("PY", "Paraguay"),
    C("QA", "Qatar"),
    C("RE", "Reunion"),
    C("RO", "Romania"),
    C("RS", "Serbia"),
    C("RU", "Russia"),
    C("RW", "Rwanda"),
    C("SA", "Saudi Arabia"),
    C("SB", "Solomon Islands"),
    C("SC", "Seychelles"),
    C("SD", "Sudan"),
    C("SE", "Sweden"),
    C("SG", "Singapore"),
    C("SH", "Saint Helena"),
    C("SI", "Slovenia"),
    C("SJ", "Svalbard and Jan Mayen"),
    C("SK", "Slovakia"),
    C("SL", "Sierra Leone"),
    C("SM", "San Marino"),
    C("SN", "Senegal"),
    C("SO", "Somalia"),
    C("SR", "Suriname"),
    C("SS", "South Sudan"),
    C("ST", "Sao Tome and Principe"),
    C("SV", "El Salvador"),
    C("SX", "Sint Maarten"),
    C("SY", "Syria"),
    C("SZ", "Eswatini"),
    C("TC", "Turks and Caicos"),
    C("TD", "Chad"),
    C("TF", "French Southern Territories"),
    C("TG", "Togo"),
    C("TH", "Thailand"),
    C("TJ", "Tajikistan"),
    C("TK", "Tokelau"),
    C("TL", "Timor-Leste"),
    C("TM", "Turkmenistan"),
    C("TN", "Tunisia"),
    C("TO", "Tonga"),
    C("TR", "Turkey"),
    C("TT", "Trinidad and Tobago"),
    C("TV", "Tuvalu"),
    C("TW", "Taiwan"),
    C("TZ", "Tanzania"),
    C("UA", "Ukraine"),
    C("UG", "Uganda"),
    C("UM", "United States Minor Outlying Islands"),
    C("US", "United States", "USA"),
    C("UY", "Uruguay"),
    C("UZ", "Uzbekistan"),
    C("VA", "Holy See"),
    C("VC", "Saint Vincent and the Grenadines"),
    C("VE", "Venezuela"),
    C("VG", "British Virgin Islands", "BVI"),
    C("VI", "U.S. Virgin Islands"),
    C("VN", "Vietnam"),
    C("VU", "Vanuatu"),
    C("WF", "Wallis and Futuna"),
    C("WS", "Samoa"),
    C("XK", "Kosovo"),
    C("YE", "Yemen"),
    C("YT", "Mayotte"),
    C("ZA", "South Africa"),
    C("ZM", "Zambia"),
    C("ZW", "Zimbabwe")
  ).sortBy(_.name) ::: List(
    // whatever
    C("EU", "European Union"),
    C("_adygea", "Adygea"),
    C("_belarus-wrw", "Belarus White-red-white"),
    C("_east-turkestan", "East Turkestan"),
    C("_lichess", "NewChess"),
    C("_pirate", "Pirate"),
    C("_rainbow", "Rainbow"),
    C("_united-nations", "United Nations"),
    C("_earth", "Earth")
  )

  val allPairs = all map { c =>
    c.code -> c.name
  }

  val map: Map[String, Country] = all.view
    .map { c =>
      c.code -> c
    }
    .to(Map)

  val nameMap: Map[Country, String] = all.view
    .map { c =>
      c -> c.name
    }
    .to(Map)

  val codeSet = map.keySet

  val nonCountries = List(
    "_lichess",
    "_pirate",
    "_rainbow",
    "_united-nations",
    "_earth"
  )

  def info(code: String): Option[Country] = map get code
  def name(country: Country): String      = nameMap.getOrElse(country, country.name)
}
