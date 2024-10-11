"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Bai, hemen dago eratze-akta (frantsesez)";i['amount']="Kopurua";i['bankTransfers']="Banku-transferentziak ere onartzen ditugu";i['becomePatron']="Lichess Babesle bihurtu";i['cancelSupport']="zure babesa ezeztatu";i['celebratedPatrons']="Hauek dira Lichess posible egiten duten Babesleak";i['changeCurrency']="Aldatu moneta";i['changeMonthlyAmount']=s("Hileroko kopurua aldatu (%s)");i['changeMonthlySupport']="Aldatu/Ezeztatu dezaket nire hileroko babesa?";i['changeOrContact']=s("Bai, edonoiz, orrialde honetatik.\nEdo %s.");i['checkOutProfile']="Ikusi zure profilaren orrialdea!";i['contactSupport']="jarri kontaktuan Lichessen arreta zerbitzuarekin";i['costBreakdown']="Ikusi kostuen banaketa";i['currentStatus']="Uneko egoera";i['date']="Data";i['decideHowMuch']="Erabaki zenbat balio duen Lichessek zuretzat:";i['donate']="Dirua eman";i['donateAsX']=s("Egin dohaintza %s gisa");i['downgradeNextMonth']="Hilabetean EZ dizugu berriz kobratuko eta zure kontua ohiko izatera pasatuko da.";i['featuresComparison']="Ikusi ezaugarrien alderaketa";i['freeAccount']="Doako kontua";i['freeChess']="Doako xakea, guztiontzat, betiko!";i['giftPatronWings']="Oparitu Babeslearen hegoak jokalari bati";i['giftPatronWingsShort']="Oparitu Babeslearen hegoak";i['ifNotRenewedThenAccountWillRevert']="Ez baduzu berritzen, zure kontua kontu arrunt bihurtuko da.";i['lichessIsRegisteredWith']=s("Lichess hemen erregistratuta dago %s.");i['lichessPatron']="Lichess babeslea";i['lifetime']="Bizi guztirako";i['lifetimePatron']="Bizi guztirako Lichess Babeslea";i['logInToDonate']="Sartu dohaintza egiteko";i['makeAdditionalDonation']="Dohaintza gehigarria orain egin";i['monthly']="Hilero";i['newPatrons']="Babesle berriak";i['nextPayment']="Hurrengo ordainketa";i['noAdsNoSubs']="Ez dago iragarkirik, ez kuotarik baina bai software librea, ordea.";i['noLongerSupport']="Lichess gehiago ez babestu";i['noPatronFeatures']="Ez, Lichess osoa doakoa delako, betiko eta guztiontzat. Hori hitz ematen dugu. Hala ere, Babesleek beren profilaren orrialdean ikono berri bat jasotzen dute.";i['nowLifetime']="Orain Bizi guztirako Lichess Babeslea zara!";i['nowOneMonth']="Orain hilabeterako Lichess Babeslea zara!";i['officialNonProfit']="Benetan da Lichess irabazi asmorik gabekoa?";i['onetime']="Behin";i['onlyDonationFromAbove']="Kontuan hartu formulario honek bakarrik emango dizula Babesle egoera.";i['otherAmount']="Bestelakoa";i['otherMethods']="Dirua emateko beste aukera batzuk?";i['patronFeatures']="Badago zerbait Babesleentzat bakarrik gordeta?";i['patronForMonths']=p({"one":"Lichess Babeslea hilabeterako","other":"Lichess Babeslea %s hilabeterako"});i['patronUntil']=s("Babesle kontua duzu egun honetara arte %s.");i['payLifetimeOnce']=s("Ordaindu %s behin. Izan Lichess Babesle betiko!");i['paymentDetails']="Ordainketaren xehetasunak";i['permanentPatron']="Orain bizi guztirako Babesle kontua duzu.";i['pleaseEnterAmountInX']=s("Idatzi kopurua %stan");i['recurringBilling']="Hileroko ordainketa, hilero berritu zure Babesle hegoak.";i['serversAndDeveloper']=s("Lehenengo, zerbitzari indartsuak.\nOndoren garatzaile baten soldata ordaintzen dugu: %s, Lichessen sortzailea.");i['singleDonation']="Behin egiteko dohaintza, Babesle egoak hilabeterako jasoko dituzu.";i['stopPayments']="Kendu zure kreditu txartela eta ordainketa egiteari utzi:";i['stopPaymentsPayPal']="Bertan behera utzi PayPal bidezko harpidetza eta utzi ordaintzeari:";i['stripeManageSub']="Kudeatu zure harpidetza eta deskargatu faktura eta ordainagiriak";i['thankYou']="Eskerrik asko zure dohaintzagatik!";i['transactionCompleted']="Zure ordainketa ondo osatu da eta ordainketa-agiria epostaz bidali dizugu.";i['tyvm']="Eskerrik asko zure laguntzagatik. Biba zu!";i['update']="Eguneratu";i['updatePaymentMethod']="Eguneratu ordainketa modua";i['viewOthers']="Beste Lichess Babesle batzuk ikusi";i['weAreNonProfit']="Irabazi asmorik gabeko elkartea gara, edonork xakean jokatzeko mundu-mailako plataforma baterako sarbidea izan beharko lukeela uste dugulako.";i['weAreSmallTeam']="Talde txikia gara, gure babesak asko lagunduko digu!";i['weRelyOnSupport']="Hau posible egiteko zu bezalako jendearen babesletza behar dugu. Lichess gustuko baduzu, gure Babesle izan zaitezke!";i['whereMoneyGoes']="Nora doa dirua?";i['withCreditCard']="Kreditu txartela";i['xBecamePatron']=s("%s Lichess Babesle bihurtu da");i['xIsPatronForNbMonths']=p({"one":"%1$s Lichess Babeslea da orain dela hilabete %2$setik","other":"%1$s Lichess Babeslea da orain dela %2$s hilabetetik"});i['xOrY']=s("%1$s edo %2$s");i['youHaveLifetime']="Bizi guztirako babesle kontua duzu. Hori ederra da!";i['youSupportWith']=s("Lichess.org babesten duzu %s-rekin hilabeterako.");i['youWillBeChargedXOnY']=s("%1$s kobratuko dizugu egun honetan %2$s.")})()