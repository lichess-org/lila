var fp =
  typeof window !== 'undefined' && window.flatpickr !== undefined
    ? window.flatpickr
    : {
        l10ns: {},
      };

var Arabic = {
  weekdays: {
    shorthand: ['أحد', 'اثنين', 'ثلاثاء', 'أربعاء', 'خميس', 'جمعة', 'سبت'],
    longhand: ['الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة', 'السبت'],
  },
  months: {
    shorthand: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'],
    longhand: [
      'يناير',
      'فبراير',
      'مارس',
      'أبريل',
      'مايو',
      'يونيو',
      'يوليو',
      'أغسطس',
      'سبتمبر',
      'أكتوبر',
      'نوفمبر',
      'ديسمبر',
    ],
  },
  firstDayOfWeek: 6,
  rangeSeparator: ' إلى ',
  weekAbbreviation: 'Wk',
  scrollTitle: 'قم بالتمرير للزيادة',
  toggleTitle: 'اضغط للتبديل',
  amPM: ['ص', 'م'],
  yearAriaLabel: 'سنة',
  monthAriaLabel: 'شهر',
  hourAriaLabel: 'ساعة',
  minuteAriaLabel: 'دقيقة',
  time_24hr: false,
};
fp.l10ns.ar = Arabic;

var Belarusian = {
  weekdays: {
    shorthand: ['Нд', 'Пн', 'Аў', 'Ср', 'Чц', 'Пт', 'Сб'],
    longhand: ['Нядзеля', 'Панядзелак', 'Аўторак', 'Серада', 'Чацвер', 'Пятніца', 'Субота'],
  },
  months: {
    shorthand: ['Сту', 'Лют', 'Сак', 'Кра', 'Тра', 'Чэр', 'Ліп', 'Жні', 'Вер', 'Кас', 'Ліс', 'Сне'],
    longhand: [
      'Студзень',
      'Люты',
      'Сакавік',
      'Красавік',
      'Травень',
      'Чэрвень',
      'Ліпень',
      'Жнівень',
      'Верасень',
      'Кастрычнік',
      'Лістапад',
      'Снежань',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '';
  },
  rangeSeparator: ' — ',
  weekAbbreviation: 'Тыд.',
  scrollTitle: 'Пракруціце для павелічэння',
  toggleTitle: 'Націсніце для пераключэння',
  amPM: ['ДП', 'ПП'],
  yearAriaLabel: 'Год',
  time_24hr: true,
};
fp.l10ns.be = Belarusian;

var Catalan = {
  weekdays: {
    shorthand: ['Dg', 'Dl', 'Dt', 'Dc', 'Dj', 'Dv', 'Ds'],
    longhand: ['Diumenge', 'Dilluns', 'Dimarts', 'Dimecres', 'Dijous', 'Divendres', 'Dissabte'],
  },
  months: {
    shorthand: ['Gen', 'Febr', 'Març', 'Abr', 'Maig', 'Juny', 'Jul', 'Ag', 'Set', 'Oct', 'Nov', 'Des'],
    longhand: [
      'Gener',
      'Febrer',
      'Març',
      'Abril',
      'Maig',
      'Juny',
      'Juliol',
      'Agost',
      'Setembre',
      'Octubre',
      'Novembre',
      'Desembre',
    ],
  },
  ordinal: function (nth) {
    var s = nth % 100;
    if (s > 3 && s < 21) return 'è';
    switch (s % 10) {
      case 1:
        return 'r';
      case 2:
        return 'n';
      case 3:
        return 'r';
      case 4:
        return 't';
      default:
        return 'è';
    }
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' a ',
  time_24hr: true,
};
fp.l10ns.ca = Catalan;

var Czech = {
  weekdays: {
    shorthand: ['Ne', 'Po', 'Út', 'St', 'Čt', 'Pá', 'So'],
    longhand: ['Neděle', 'Pondělí', 'Úterý', 'Středa', 'Čtvrtek', 'Pátek', 'Sobota'],
  },
  months: {
    shorthand: ['Led', 'Ún', 'Bře', 'Dub', 'Kvě', 'Čer', 'Čvc', 'Srp', 'Zář', 'Říj', 'Lis', 'Pro'],
    longhand: [
      'Leden',
      'Únor',
      'Březen',
      'Duben',
      'Květen',
      'Červen',
      'Červenec',
      'Srpen',
      'Září',
      'Říjen',
      'Listopad',
      'Prosinec',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '.';
  },
  rangeSeparator: ' do ',
  weekAbbreviation: 'Týd.',
  scrollTitle: 'Rolujte pro změnu',
  toggleTitle: 'Přepnout dopoledne/odpoledne',
  amPM: ['dop.', 'odp.'],
  yearAriaLabel: 'Rok',
  time_24hr: true,
};
fp.l10ns.cs = Czech;

var Danish = {
  weekdays: {
    shorthand: ['søn', 'man', 'tir', 'ons', 'tors', 'fre', 'lør'],
    longhand: ['søndag', 'mandag', 'tirsdag', 'onsdag', 'torsdag', 'fredag', 'lørdag'],
  },
  months: {
    shorthand: ['jan', 'feb', 'mar', 'apr', 'maj', 'jun', 'jul', 'aug', 'sep', 'okt', 'nov', 'dec'],
    longhand: [
      'januar',
      'februar',
      'marts',
      'april',
      'maj',
      'juni',
      'juli',
      'august',
      'september',
      'oktober',
      'november',
      'december',
    ],
  },
  ordinal: function () {
    return '.';
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' til ',
  weekAbbreviation: 'uge',
  time_24hr: true,
};
fp.l10ns.da = Danish;

var German = {
  weekdays: {
    shorthand: ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'],
    longhand: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'],
    longhand: [
      'Januar',
      'Februar',
      'März',
      'April',
      'Mai',
      'Juni',
      'Juli',
      'August',
      'September',
      'Oktober',
      'November',
      'Dezember',
    ],
  },
  firstDayOfWeek: 1,
  weekAbbreviation: 'KW',
  rangeSeparator: ' bis ',
  scrollTitle: 'Zum Ändern scrollen',
  toggleTitle: 'Zum Umschalten klicken',
  time_24hr: true,
};
fp.l10ns.de = German;

var Spanish = {
  weekdays: {
    shorthand: ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'],
    longhand: ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'],
  },
  months: {
    shorthand: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
    longhand: [
      'Enero',
      'Febrero',
      'Marzo',
      'Abril',
      'Mayo',
      'Junio',
      'Julio',
      'Agosto',
      'Septiembre',
      'Octubre',
      'Noviembre',
      'Diciembre',
    ],
  },
  ordinal: function () {
    return 'º';
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' a ',
  time_24hr: true,
};
fp.l10ns.es = Spanish;

var Persian = {
  weekdays: {
    shorthand: ['یک', 'دو', 'سه', 'چهار', 'پنج', 'جمعه', 'شنبه'],
    longhand: ['یک‌شنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنچ‌شنبه', 'جمعه', 'شنبه'],
  },
  months: {
    shorthand: [
      'ژانویه',
      'فوریه',
      'مارس',
      'آوریل',
      'مه',
      'ژوئن',
      'ژوئیه',
      'اوت',
      'سپتامبر',
      'اکتبر',
      'نوامبر',
      'دسامبر',
    ],
    longhand: [
      'ژانویه',
      'فوریه',
      'مارس',
      'آوریل',
      'مه',
      'ژوئن',
      'ژوئیه',
      'اوت',
      'سپتامبر',
      'اکتبر',
      'نوامبر',
      'دسامبر',
    ],
  },
  firstDayOfWeek: 6,
  ordinal: function () {
    return '';
  },
};
fp.l10ns.fa = Persian;

var Finnish = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['su', 'ma', 'ti', 'ke', 'to', 'pe', 'la'],
    longhand: ['sunnuntai', 'maanantai', 'tiistai', 'keskiviikko', 'torstai', 'perjantai', 'lauantai'],
  },
  months: {
    shorthand: [
      'tammi',
      'helmi',
      'maalis',
      'huhti',
      'touko',
      'kesä',
      'heinä',
      'elo',
      'syys',
      'loka',
      'marras',
      'joulu',
    ],
    longhand: [
      'tammikuu',
      'helmikuu',
      'maaliskuu',
      'huhtikuu',
      'toukokuu',
      'kesäkuu',
      'heinäkuu',
      'elokuu',
      'syyskuu',
      'lokakuu',
      'marraskuu',
      'joulukuu',
    ],
  },
  ordinal: function () {
    return '.';
  },
  time_24hr: true,
};
fp.l10ns.fi = Finnish;

var French = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['dim', 'lun', 'mar', 'mer', 'jeu', 'ven', 'sam'],
    longhand: ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'],
  },
  months: {
    shorthand: ['janv', 'févr', 'mars', 'avr', 'mai', 'juin', 'juil', 'août', 'sept', 'oct', 'nov', 'déc'],
    longhand: [
      'janvier',
      'février',
      'mars',
      'avril',
      'mai',
      'juin',
      'juillet',
      'août',
      'septembre',
      'octobre',
      'novembre',
      'décembre',
    ],
  },
  ordinal: function (nth) {
    if (nth > 1) return '';
    return 'er';
  },
  rangeSeparator: ' au ',
  weekAbbreviation: 'Sem',
  scrollTitle: 'Défiler pour augmenter la valeur',
  toggleTitle: 'Cliquer pour basculer',
  time_24hr: true,
};
fp.l10ns.fr = French;

var Greek = {
  weekdays: {
    shorthand: ['Κυ', 'Δε', 'Τρ', 'Τε', 'Πέ', 'Πα', 'Σά'],
    longhand: ['Κυριακή', 'Δευτέρα', 'Τρίτη', 'Τετάρτη', 'Πέμπτη', 'Παρασκευή', 'Σάββατο'],
  },
  months: {
    shorthand: ['Ιαν', 'Φεβ', 'Μάρ', 'Απρ', 'Μάι', 'Ιούν', 'Ιούλ', 'Αύγ', 'Σεπ', 'Οκτ', 'Νοέ', 'Δεκ'],
    longhand: [
      'Ιανουάριος',
      'Φεβρουάριος',
      'Μάρτιος',
      'Απρίλιος',
      'Μάιος',
      'Ιούνιος',
      'Ιούλιος',
      'Αύγουστος',
      'Σεπτέμβριος',
      'Οκτώβριος',
      'Νοέμβριος',
      'Δεκέμβριος',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '';
  },
  weekAbbreviation: 'Εβδ',
  rangeSeparator: ' έως ',
  scrollTitle: 'Μετακυλήστε για προσαύξηση',
  toggleTitle: 'Κάντε κλικ για αλλαγή',
  amPM: ['ΠΜ', 'ΜΜ'],
  yearAriaLabel: 'χρόνος',
  monthAriaLabel: 'μήνας',
  hourAriaLabel: 'ώρα',
  minuteAriaLabel: 'λεπτό',
};
fp.l10ns.el = Greek;

var Hebrew = {
  weekdays: {
    shorthand: ['א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ש'],
    longhand: ['ראשון', 'שני', 'שלישי', 'רביעי', 'חמישי', 'שישי', 'שבת'],
  },
  months: {
    shorthand: ['ינו׳', 'פבר׳', 'מרץ', 'אפר׳', 'מאי', 'יוני', 'יולי', 'אוג׳', 'ספט׳', 'אוק׳', 'נוב׳', 'דצמ׳'],
    longhand: [
      'ינואר',
      'פברואר',
      'מרץ',
      'אפריל',
      'מאי',
      'יוני',
      'יולי',
      'אוגוסט',
      'ספטמבר',
      'אוקטובר',
      'נובמבר',
      'דצמבר',
    ],
  },
  rangeSeparator: ' אל ',
  time_24hr: true,
};
fp.l10ns.he = Hebrew;

var Hindi = {
  weekdays: {
    shorthand: ['रवि', 'सोम', 'मंगल', 'बुध', 'गुरु', 'शुक्र', 'शनि'],
    longhand: ['रविवार', 'सोमवार', 'मंगलवार', 'बुधवार', 'गुरुवार', 'शुक्रवार', 'शनिवार'],
  },
  months: {
    shorthand: ['जन', 'फर', 'मार्च', 'अप्रेल', 'मई', 'जून', 'जूलाई', 'अग', 'सित', 'अक्ट', 'नव', 'दि'],
    longhand: [
      'जनवरी ',
      'फरवरी',
      'मार्च',
      'अप्रेल',
      'मई',
      'जून',
      'जूलाई',
      'अगस्त ',
      'सितम्बर',
      'अक्टूबर',
      'नवम्बर',
      'दिसम्बर',
    ],
  },
};
fp.l10ns.hi = Hindi;

var Croatian = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['Ned', 'Pon', 'Uto', 'Sri', 'Čet', 'Pet', 'Sub'],
    longhand: ['Nedjelja', 'Ponedjeljak', 'Utorak', 'Srijeda', 'Četvrtak', 'Petak', 'Subota'],
  },
  months: {
    shorthand: ['Sij', 'Velj', 'Ožu', 'Tra', 'Svi', 'Lip', 'Srp', 'Kol', 'Ruj', 'Lis', 'Stu', 'Pro'],
    longhand: [
      'Siječanj',
      'Veljača',
      'Ožujak',
      'Travanj',
      'Svibanj',
      'Lipanj',
      'Srpanj',
      'Kolovoz',
      'Rujan',
      'Listopad',
      'Studeni',
      'Prosinac',
    ],
  },
  time_24hr: true,
};
fp.l10ns.hr = Croatian;

var Hungarian = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['V', 'H', 'K', 'Sz', 'Cs', 'P', 'Szo'],
    longhand: ['Vasárnap', 'Hétfő', 'Kedd', 'Szerda', 'Csütörtök', 'Péntek', 'Szombat'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Már', 'Ápr', 'Máj', 'Jún', 'Júl', 'Aug', 'Szep', 'Okt', 'Nov', 'Dec'],
    longhand: [
      'Január',
      'Február',
      'Március',
      'Április',
      'Május',
      'Június',
      'Július',
      'Augusztus',
      'Szeptember',
      'Október',
      'November',
      'December',
    ],
  },
  ordinal: function () {
    return '.';
  },
  weekAbbreviation: 'Hét',
  scrollTitle: 'Görgessen',
  toggleTitle: 'Kattintson a váltáshoz',
  rangeSeparator: ' - ',
  time_24hr: true,
};
fp.l10ns.hu = Hungarian;

var Indonesian = {
  weekdays: {
    shorthand: ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab'],
    longhand: ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'],
    longhand: [
      'Januari',
      'Februari',
      'Maret',
      'April',
      'Mei',
      'Juni',
      'Juli',
      'Agustus',
      'September',
      'Oktober',
      'November',
      'Desember',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '';
  },
  time_24hr: true,
  rangeSeparator: ' - ',
};
fp.l10ns.id = Indonesian;

var Icelandic = {
  weekdays: {
    shorthand: ['Sun', 'Mán', 'Þri', 'Mið', 'Fim', 'Fös', 'Lau'],
    longhand: ['Sunnudagur', 'Mánudagur', 'Þriðjudagur', 'Miðvikudagur', 'Fimmtudagur', 'Föstudagur', 'Laugardagur'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mar', 'Apr', 'Maí', 'Jún', 'Júl', 'Ágú', 'Sep', 'Okt', 'Nóv', 'Des'],
    longhand: [
      'Janúar',
      'Febrúar',
      'Mars',
      'Apríl',
      'Maí',
      'Júní',
      'Júlí',
      'Ágúst',
      'September',
      'Október',
      'Nóvember',
      'Desember',
    ],
  },
  ordinal: function () {
    return '.';
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' til ',
  weekAbbreviation: 'vika',
  yearAriaLabel: 'Ár',
  time_24hr: true,
};
fp.l10ns.is = Icelandic;

var Italian = {
  weekdays: {
    shorthand: ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab'],
    longhand: ['Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato'],
  },
  months: {
    shorthand: ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'],
    longhand: [
      'Gennaio',
      'Febbraio',
      'Marzo',
      'Aprile',
      'Maggio',
      'Giugno',
      'Luglio',
      'Agosto',
      'Settembre',
      'Ottobre',
      'Novembre',
      'Dicembre',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '°';
  },
  rangeSeparator: ' al ',
  weekAbbreviation: 'Se',
  scrollTitle: 'Scrolla per aumentare',
  toggleTitle: 'Clicca per cambiare',
  time_24hr: true,
};
fp.l10ns.it = Italian;

var Japanese = {
  weekdays: {
    shorthand: ['日', '月', '火', '水', '木', '金', '土'],
    longhand: ['日曜日', '月曜日', '火曜日', '水曜日', '木曜日', '金曜日', '土曜日'],
  },
  months: {
    shorthand: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
    longhand: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
  },
  time_24hr: true,
  rangeSeparator: ' から ',
  monthAriaLabel: '月',
  amPM: ['午前', '午後'],
  yearAriaLabel: '年',
  hourAriaLabel: '時間',
  minuteAriaLabel: '分',
};
fp.l10ns.ja = Japanese;

var Korean = {
  weekdays: {
    shorthand: ['일', '월', '화', '수', '목', '금', '토'],
    longhand: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
  },
  months: {
    shorthand: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
    longhand: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
  },
  ordinal: function () {
    return '일';
  },
  rangeSeparator: ' ~ ',
  amPM: ['오전', '오후'],
};
fp.l10ns.ko = Korean;

var Lithuanian = {
  weekdays: {
    shorthand: ['S', 'Pr', 'A', 'T', 'K', 'Pn', 'Š'],
    longhand: [
      'Sekmadienis',
      'Pirmadienis',
      'Antradienis',
      'Trečiadienis',
      'Ketvirtadienis',
      'Penktadienis',
      'Šeštadienis',
    ],
  },
  months: {
    shorthand: ['Sau', 'Vas', 'Kov', 'Bal', 'Geg', 'Bir', 'Lie', 'Rgp', 'Rgs', 'Spl', 'Lap', 'Grd'],
    longhand: [
      'Sausis',
      'Vasaris',
      'Kovas',
      'Balandis',
      'Gegužė',
      'Birželis',
      'Liepa',
      'Rugpjūtis',
      'Rugsėjis',
      'Spalis',
      'Lapkritis',
      'Gruodis',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '-a';
  },
  rangeSeparator: ' iki ',
  weekAbbreviation: 'Sav',
  scrollTitle: 'Keisti laiką pelės rateliu',
  toggleTitle: 'Perjungti laiko formatą',
  time_24hr: true,
};
fp.l10ns.lt = Lithuanian;

var Latvian = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['Sv', 'Pr', 'Ot', 'Tr', 'Ce', 'Pk', 'Se'],
    longhand: ['Svētdiena', 'Pirmdiena', 'Otrdiena', 'Trešdiena', 'Ceturtdiena', 'Piektdiena', 'Sestdiena'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mar', 'Apr', 'Mai', 'Jūn', 'Jūl', 'Aug', 'Sep', 'Okt', 'Nov', 'Dec'],
    longhand: [
      'Janvāris',
      'Februāris',
      'Marts',
      'Aprīlis',
      'Maijs',
      'Jūnijs',
      'Jūlijs',
      'Augusts',
      'Septembris',
      'Oktobris',
      'Novembris',
      'Decembris',
    ],
  },
  rangeSeparator: ' līdz ',
  time_24hr: true,
};
fp.l10ns.lv = Latvian;

var Dutch = {
  weekdays: {
    shorthand: ['zo', 'ma', 'di', 'wo', 'do', 'vr', 'za'],
    longhand: ['zondag', 'maandag', 'dinsdag', 'woensdag', 'donderdag', 'vrijdag', 'zaterdag'],
  },
  months: {
    shorthand: ['jan', 'feb', 'mrt', 'apr', 'mei', 'jun', 'jul', 'aug', 'sept', 'okt', 'nov', 'dec'],
    longhand: [
      'januari',
      'februari',
      'maart',
      'april',
      'mei',
      'juni',
      'juli',
      'augustus',
      'september',
      'oktober',
      'november',
      'december',
    ],
  },
  firstDayOfWeek: 1,
  weekAbbreviation: 'wk',
  rangeSeparator: ' t/m ',
  scrollTitle: 'Scroll voor volgende / vorige',
  toggleTitle: 'Klik om te wisselen',
  time_24hr: true,
  ordinal: function (nth) {
    if (nth === 1 || nth === 8 || nth >= 20) return 'ste';
    return 'de';
  },
};
fp.l10ns.nl = Dutch;

var Norwegian = {
  weekdays: {
    shorthand: ['Søn', 'Man', 'Tir', 'Ons', 'Tor', 'Fre', 'Lør'],
    longhand: ['Søndag', 'Mandag', 'Tirsdag', 'Onsdag', 'Torsdag', 'Fredag', 'Lørdag'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mar', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Des'],
    longhand: [
      'Januar',
      'Februar',
      'Mars',
      'April',
      'Mai',
      'Juni',
      'Juli',
      'August',
      'September',
      'Oktober',
      'November',
      'Desember',
    ],
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' til ',
  weekAbbreviation: 'Uke',
  scrollTitle: 'Scroll for å endre',
  toggleTitle: 'Klikk for å veksle',
  time_24hr: true,
  ordinal: function () {
    return '.';
  },
};
fp.l10ns.nb = Norwegian;

var Polish = {
  weekdays: {
    shorthand: ['Nd', 'Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So'],
    longhand: ['Niedziela', 'Poniedziałek', 'Wtorek', 'Środa', 'Czwartek', 'Piątek', 'Sobota'],
  },
  months: {
    shorthand: ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz', 'Paź', 'Lis', 'Gru'],
    longhand: [
      'Styczeń',
      'Luty',
      'Marzec',
      'Kwiecień',
      'Maj',
      'Czerwiec',
      'Lipiec',
      'Sierpień',
      'Wrzesień',
      'Październik',
      'Listopad',
      'Grudzień',
    ],
  },
  rangeSeparator: ' do ',
  weekAbbreviation: 'tydz.',
  scrollTitle: 'Przewiń, aby zwiększyć',
  toggleTitle: 'Kliknij, aby przełączyć',
  firstDayOfWeek: 1,
  time_24hr: true,
  ordinal: function () {
    return '.';
  },
};
fp.l10ns.pl = Polish;

var Portuguese = {
  weekdays: {
    shorthand: ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'],
    longhand: ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'],
  },
  months: {
    shorthand: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'],
    longhand: [
      'Janeiro',
      'Fevereiro',
      'Março',
      'Abril',
      'Maio',
      'Junho',
      'Julho',
      'Agosto',
      'Setembro',
      'Outubro',
      'Novembro',
      'Dezembro',
    ],
  },
  rangeSeparator: ' até ',
  time_24hr: true,
};
fp.l10ns.pt = Portuguese;

var Romanian = {
  weekdays: {
    shorthand: ['Dum', 'Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sâm'],
    longhand: ['Duminică', 'Luni', 'Marți', 'Miercuri', 'Joi', 'Vineri', 'Sâmbătă'],
  },
  months: {
    shorthand: ['Ian', 'Feb', 'Mar', 'Apr', 'Mai', 'Iun', 'Iul', 'Aug', 'Sep', 'Oct', 'Noi', 'Dec'],
    longhand: [
      'Ianuarie',
      'Februarie',
      'Martie',
      'Aprilie',
      'Mai',
      'Iunie',
      'Iulie',
      'August',
      'Septembrie',
      'Octombrie',
      'Noiembrie',
      'Decembrie',
    ],
  },
  firstDayOfWeek: 1,
  time_24hr: true,
  ordinal: function () {
    return '';
  },
};
fp.l10ns.ro = Romanian;

var Russian = {
  weekdays: {
    shorthand: ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'],
    longhand: ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'],
  },
  months: {
    shorthand: ['Янв', 'Фев', 'Март', 'Апр', 'Май', 'Июнь', 'Июль', 'Авг', 'Сен', 'Окт', 'Ноя', 'Дек'],
    longhand: [
      'Январь',
      'Февраль',
      'Март',
      'Апрель',
      'Май',
      'Июнь',
      'Июль',
      'Август',
      'Сентябрь',
      'Октябрь',
      'Ноябрь',
      'Декабрь',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '';
  },
  rangeSeparator: ' — ',
  weekAbbreviation: 'Нед.',
  scrollTitle: 'Прокрутите для увеличения',
  toggleTitle: 'Нажмите для переключения',
  amPM: ['ДП', 'ПП'],
  yearAriaLabel: 'Год',
  time_24hr: true,
};
fp.l10ns.ru = Russian;

var Serbian = {
  weekdays: {
    shorthand: ['Ned', 'Pon', 'Uto', 'Sre', 'Čet', 'Pet', 'Sub'],
    longhand: ['Nedelja', 'Ponedeljak', 'Utorak', 'Sreda', 'Četvrtak', 'Petak', 'Subota'],
  },
  months: {
    shorthand: ['Jan', 'Feb', 'Mar', 'Apr', 'Maj', 'Jun', 'Jul', 'Avg', 'Sep', 'Okt', 'Nov', 'Dec'],
    longhand: [
      'Januar',
      'Februar',
      'Mart',
      'April',
      'Maj',
      'Jun',
      'Jul',
      'Avgust',
      'Septembar',
      'Oktobar',
      'Novembar',
      'Decembar',
    ],
  },
  firstDayOfWeek: 1,
  weekAbbreviation: 'Ned.',
  rangeSeparator: ' do ',
  time_24hr: true,
};
fp.l10ns.sr = Serbian;

var Swedish = {
  firstDayOfWeek: 1,
  weekAbbreviation: 'v',
  weekdays: {
    shorthand: ['sön', 'mån', 'tis', 'ons', 'tor', 'fre', 'lör'],
    longhand: ['söndag', 'måndag', 'tisdag', 'onsdag', 'torsdag', 'fredag', 'lördag'],
  },
  months: {
    shorthand: ['jan', 'feb', 'mar', 'apr', 'maj', 'jun', 'jul', 'aug', 'sep', 'okt', 'nov', 'dec'],
    longhand: [
      'januari',
      'februari',
      'mars',
      'april',
      'maj',
      'juni',
      'juli',
      'augusti',
      'september',
      'oktober',
      'november',
      'december',
    ],
  },
  rangeSeparator: ' till ',
  time_24hr: true,
  ordinal: function () {
    return '.';
  },
};
fp.l10ns.sv = Swedish;

var Thai = {
  weekdays: {
    shorthand: ['อา', 'จ', 'อ', 'พ', 'พฤ', 'ศ', 'ส'],
    longhand: ['อาทิตย์', 'จันทร์', 'อังคาร', 'พุธ', 'พฤหัสบดี', 'ศุกร์', 'เสาร์'],
  },
  months: {
    shorthand: ['ม.ค.', 'ก.พ.', 'มี.ค.', 'เม.ย.', 'พ.ค.', 'มิ.ย.', 'ก.ค.', 'ส.ค.', 'ก.ย.', 'ต.ค.', 'พ.ย.', 'ธ.ค.'],
    longhand: [
      'มกราคม',
      'กุมภาพันธ์',
      'มีนาคม',
      'เมษายน',
      'พฤษภาคม',
      'มิถุนายน',
      'กรกฎาคม',
      'สิงหาคม',
      'กันยายน',
      'ตุลาคม',
      'พฤศจิกายน',
      'ธันวาคม',
    ],
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' ถึง ',
  scrollTitle: 'เลื่อนเพื่อเพิ่มหรือลด',
  toggleTitle: 'คลิกเพื่อเปลี่ยน',
  time_24hr: true,
  ordinal: function () {
    return '';
  },
};
fp.l10ns.th = Thai;

var Turkish = {
  weekdays: {
    shorthand: ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'],
    longhand: ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'],
  },
  months: {
    shorthand: ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'],
    longhand: [
      'Ocak',
      'Şubat',
      'Mart',
      'Nisan',
      'Mayıs',
      'Haziran',
      'Temmuz',
      'Ağustos',
      'Eylül',
      'Ekim',
      'Kasım',
      'Aralık',
    ],
  },
  firstDayOfWeek: 1,
  ordinal: function () {
    return '.';
  },
  rangeSeparator: ' - ',
  weekAbbreviation: 'Hf',
  scrollTitle: 'Artırmak için kaydırın',
  toggleTitle: 'Aç/Kapa',
  amPM: ['ÖÖ', 'ÖS'],
  time_24hr: true,
};
fp.l10ns.tr = Turkish;

var Ukrainian = {
  firstDayOfWeek: 1,
  weekdays: {
    shorthand: ['Нд', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'],
    longhand: ['Неділя', 'Понеділок', 'Вівторок', 'Середа', 'Четвер', "П'ятниця", 'Субота'],
  },
  months: {
    shorthand: ['Січ', 'Лют', 'Бер', 'Кві', 'Тра', 'Чер', 'Лип', 'Сер', 'Вер', 'Жов', 'Лис', 'Гру'],
    longhand: [
      'Січень',
      'Лютий',
      'Березень',
      'Квітень',
      'Травень',
      'Червень',
      'Липень',
      'Серпень',
      'Вересень',
      'Жовтень',
      'Листопад',
      'Грудень',
    ],
  },
  time_24hr: true,
};
fp.l10ns.uk = Ukrainian;

var Vietnamese = {
  weekdays: {
    shorthand: ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'],
    longhand: ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'],
  },
  months: {
    shorthand: ['Th1', 'Th2', 'Th3', 'Th4', 'Th5', 'Th6', 'Th7', 'Th8', 'Th9', 'Th10', 'Th11', 'Th12'],
    longhand: [
      'Tháng một',
      'Tháng hai',
      'Tháng ba',
      'Tháng tư',
      'Tháng năm',
      'Tháng sáu',
      'Tháng bảy',
      'Tháng tám',
      'Tháng chín',
      'Tháng mười',
      'Tháng mười một',
      'Tháng mười hai',
    ],
  },
  firstDayOfWeek: 1,
  rangeSeparator: ' đến ',
};
fp.l10ns.vi = Vietnamese;

var Mandarin = {
  weekdays: {
    shorthand: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
    longhand: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
  },
  months: {
    shorthand: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
    longhand: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
  },
  rangeSeparator: ' 至 ',
  weekAbbreviation: '周',
  scrollTitle: '滚动切换',
  toggleTitle: '点击切换 12/24 小时时制',
};
fp.l10ns['zh-Hans'] = Mandarin;

var MandarinTraditional = {
  weekdays: {
    shorthand: ['週日', '週一', '週二', '週三', '週四', '週五', '週六'],
    longhand: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
  },
  months: {
    shorthand: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
    longhand: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
  },
  rangeSeparator: ' 至 ',
  weekAbbreviation: '週',
  scrollTitle: '滾動切換',
  toggleTitle: '點擊切換 12/24 小時時制',
};
fp.l10ns['zh-Hant'] = MandarinTraditional;
