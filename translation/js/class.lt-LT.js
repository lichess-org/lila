"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="Pridėkite Lichess vartotojų vardus norėdami pakviesti kaip mokytojus. Vienas vardas per eilutę.";i['addStudent']="Pridėti mokinį";i['aLinkToTheClassWillBeAdded']="Nuoroda į klasę bus automatiškai pridėta žinutės gale. Jums patiems jos pridėti nereikia.";i['anInvitationHasBeenSentToX']=s("Pakvietimas buvo išsiųstas %s");i['applyToBeLichessTeacher']="Tapkite Lichess mokytoju";i['classDescription']="Klasės aprašymas";i['className']="Klasės pavadinimas";i['classNews']="Klasės naujienos";i['clickToViewInvitation']="Spustelėkite nuorodą norėdami peržiūrėti pakvietimą:";i['closeClass']="Uždaryti klasę";i['closeDesc1']="Mokinys nebegalės prieiti prie šios paskyros. Uždarymas yra neatstatomas. Įsitikinkite, kad mokinys supranta ir sutinka.";i['closeDesc2']="Jūs taip pat galite paskyros valdymą perleisti mokiniui. Taip jie galės toliau naudotis savo paskyra.";i['closeStudent']="Uždaryti paskyrą";i['closeTheAccount']="Visam laikui uždaro mokinio paskyrą.";i['createANewLichessAccount']="Kurti naują Lichess paskyrą";i['createDesc1']="Jei mokinys dar neturi Lichess paskyros, galite jam tokią sukurti čia.";i['createDesc2']="El. pašto adresas nereikalingas. Bus sugeneruotas slaptažodis, kurį turėsite perduoti mokiniui. Šiuo slaptažodžiu jis galės prisijungti.";i['createDesc3']="Svarbu: mokinys negali turėti kelių paskyrų.";i['createDesc4']="Jei jis jau turi paskyrą, naudokite pakvietimo formą.";i['createMoreClasses']="sukurkite daugiau klasių";i['createMultipleAccounts']="Sukurti kelias Lichess paskyras iš karto";i['createStudentWarning']="Kurkite paskyras tik tikriems mokiniams. Nenaudokite šios funkcijos kurdami kelias paskyras sau, kitu atveju būsite užblokuoti.";i['editNews']="Keisti naujienas";i['features']="Funkcijos";i['freeForAllForever']="100% nemokamas visiems ir visada, jokių reklamų ar sekimo įskiepių";i['generateANewPassword']="Sugeneruoti naują slaptažodį mokiniui";i['generateANewUsername']="Sugeneruoti naują vartotojo vardą";i['invitationToClass']=s("Jūs buvote pakviestas prisijungti prie klasės \\\"%s\\\" kaip mokinys.");i['invite']="Pakviesti";i['inviteALichessAccount']="Pakviesti Lichess vartotoją";i['inviteDesc1']="Jei mokinys jau turi Lichess paskyrą, galite jį pakviesti į klasę.";i['inviteDesc2']="Jie gaus žinutę su prisijungimo nuoroda per Lichess.";i['inviteDesc3']="Svarbu: pakvieskite tik mokinius, kuriuos pažįstate, ir kurie nori dalyvauti jūsų klasėje.";i['inviteDesc4']="Niekada nesiųskite pakvietimų atsitiktiniams žaidėjams.";i['invitedToXByY']=s("%1$s pakviestas į %2$s");i['inviteTheStudentBack']="Pakviesti mokinį atgal";i['lastActiveDate']="Aktyvus";i['lichessClasses']="Klasės";i['lichessProfileXCreatedForY']=s("Lichess profilis %1$s sukurtas %2$s.");i['lichessUsername']="Lichess vartotojo vardas";i['makeSureToCopy']="Dabar būtinai nusikopijuokite ar nusirašykite slaptažodį. Daugiau jo nerodysime!";i['managed']="Valdoma";i['maxStudentsNote']=s("Norime pažymėti, kad klasės gali turėti iki %1$s mokinių. Norėdami valdyti daugiau mokinių, %2$s.");i['messageAllStudents']="Praneškite visiems mokiniams apie naują pamokų medžiagą";i['multipleAccsFormDescription']=s("Norėdami sukurti kelias Lichess paskyras iš mokinių vardų galite %s.");i['na']="Neprieinama";i['nbPendingInvitations']=p({"one":"Vienas laukiantis kvietimas","few":"%s laukiantys kvietimai","many":"%s laukiančio kvietimo","other":"%s laukiančių kvietimų"});i['nbStudents']=p({"one":"Mokinys","few":"%s mokiniai","many":"%s mokinio","other":"%s mokinių"});i['nbTeachers']=p({"one":"Mokytojas","few":"%s mokytojai","many":"%s mokytojo","other":"%s mokytojų"});i['newClass']="Nauja klasė";i['news']="Naujienos";i['newsEdit1']="Visos klasės naujienos viename laukelyje.";i['newsEdit2']="Paskutines naujienas pridėkite viršuje. Netrinkite esamų naujienų.";i['newsEdit3']="Atskirkite naujienas su ---\nTaip bus parodyta horizontali linija.";i['noClassesYet']="Dar nėra klasių.";i['noRemovedStudents']="Nėra pašalintų mokinių.";i['noStudents']="Klasėje dar nėra mokinių.";i['nothingHere']="Kol kas nieko.";i['notifyAllStudents']="Pranešti visiems mokiniams";i['onlyVisibleToTeachers']="Matoma tik klasės mokytojams";i['orSeparator']="arba";i['overDays']="Per dienas";i['overview']="Apžvalga";i['passwordX']=s("Slaptažodis: %s");i['privateWillNeverBeShown']="Privatu. Nebus rodoma už klasės ribų. Padeda prisiminti, koks tai mokinys.";i['progress']="Progresas";i['quicklyGenerateSafeUsernames']="Greitai mokiniams sugeneruokite saugius vartotojų vardus ir slaptažodžius";i['realName']="Tikras vardas";i['realUniqueEmail']="Tikras, unikalus mokinio el. pašto adresas. Juo išsiųsime patvirtinimo laišką su nuoroda išleisti paskyrą.";i['release']="Išleisti";i['releaseDesc1']="Išleista paskyra nebegali tapti valdoma. Mokinys galės perjungi vaikų režimą ar pakeisti savo slaptažodį.";i['releaseDesc2']="Po paskyros paleidimo mokinys liks klasėje.";i['releaseTheAccount']="Išleidus paskyrą mokinys ją gali valdyti savarankiškai.";i['removedByX']=s("Pašalinta %s");i['removedStudents']="Pašalinti";i['removeStudent']="Pašalinti mokinį";i['reopen']="Atidaryti vėl";i['resetPassword']="Atstatyti slaptažodį";i['sendAMessage']="Siųsti žinutę visiems mokiniams.";i['studentCredentials']=s("Mokinys: %1$s\nVartotojo vardas: %2$s\nSlaptažodis: %3$s");i['students']="Mokiniai";i['studentsRealNamesOnePerLine']="Mokinių tikri vardai, vienas per eilutę";i['teachClassesOfChessStudents']="Mokykite šachmatų mokinių klases su Lichess klasių įrankių paketu.";i['teachers']="Mokytojai";i['teachersOfTheClass']="Klasės mokytojai";i['teachersX']=s("Mokytojai: %s");i['thisStudentAccountIsManaged']="Ši mokinio paskyra valdoma";i['timePlaying']="Žaista laiko";i['trackStudentProgress']="Sekite mokinių progresą partijose ir galvosūkiuose";i['upgradeFromManaged']="Keisti iš valdomos į autonominę";i['useThisForm']="naudoti šią formą";i['variantXOverLastY']=s("%1$s per paskutines %2$s");i['visibleByBothStudentsAndTeachers']="Matoma ir klasės mokytojams ir klasės mokiniams";i['welcomeToClass']=s("Sveiki atvykę į klasę: %s\nŠtai nuoroda patekti į klasę.");i['winrate']="Laimėjimo dažnumas";i['xAlreadyHasAPendingInvitation']=s("%s jau turi laukiantį pakvietimą");i['xIsAKidAccountWarning']=s("%1$s yra vaiko paskyra ir negali priimti jūsų žinutės. Turite jiems duoti pakvietimo adresą kitu būdu: %2$s");i['xisNowAStudentOfTheClass']=s("%s yra klasės mokinys");i['youAcceptedThisInvitation']="Jūs priėmėte šį pakvietimą.";i['youDeclinedThisInvitation']="Jūs atsisakėte šio pakvietimo.";i['youHaveBeenInvitedByX']=s("Jūs buvote pakviestas(-a) %s.")})()