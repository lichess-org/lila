"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="Ouzhpennit o anvioù implijer Lichess evit pediñ anezhe da gelennerien. Unan dre linenn.";i['addStudent']="Ouzhpennañ ur studier";i['aLinkToTheClassWillBeAdded']="Ul liamm emgefreek a vo ouzhpennet e traoù ar gemennadenn ha gant se n\\'ho peus ket c\\'hwi d\\'e begañ.";i['applyToBeLichessTeacher']="Lakaat e anv evit bezañ kelenner";i['classDescription']="Deskrivadur ar c\\'hlas";i['className']="Anv ar c\\'hlas";i['clickToViewInvitation']="Klikit war al liamm da welet ar bedadenn:";i['closeClass']="Serriñ ar c\\'hlas";i['closeDesc1']="Ne c\\'hallo ket ar studier implijout ar gont-mañ ken. Peurbadus eo serriñ. Bezit sur eo bet komprenet hag asantet gant ar studier.";i['closeStudent']="Serriñ ar gont";i['closeTheAccount']="Serriñ kont ar studier da viken.";i['createANewLichessAccount']="Krouiñ ur gont Lichess nevez";i['createDesc1']="Ma n\\'en deus ar studier kont Lichess ebet e c\\'hallit krouiñ unan evitañ amañ.";i['createDesc2']="N\\'eo ket ret kaout ur chomlec\\'h postel. Krouet e vo ur ger-tremen a vo fiziet en ho studier ganeoc\\'h evit ma c\\'hallfe kevreañ.";i['createDesc3']="Da ziwall: ne c\\'hall ket ur studier kaout meur a gont.";i['createDesc4']="Grit gant ar furmskrid pediñ m\\'o deus unan dija.";i['editNews']="Cheñch ar c\\'heleier";i['features']="Arc\\'hweladurioù";i['freeForAllForever']="100% digoust evit an holl, da viken, hep bruderezh na trackers";i['generateANewPassword']="Krouiñ ur ger-tremen nevez evit ar studier";i['generateANewUsername']="Krouiñ ur ger-tremen nevez";i['invitationToClass']=s("Pedet oc\\'h da vezañ ezel eus ar c\\'hlas \\\"%s\\\" evel studier.");i['inviteALichessAccount']="Pediñ ur gont Lichess";i['inviteDesc1']="Ma en deus ar studier ur gont Lichess dija e c\\'hallit e bediñ da gemer perzh er c\\'hlas.";i['inviteDesc2']="Ur gemennadenn gant liamm evit mont er c\\'hlas ennañ a vo kaset dezhe.";i['inviteDesc3']="A-bouez: pedit studierien anavezet ganeoc\\'h hag a fell dezhe emezelañ da vat.";i['inviteDesc4']="Arabat pediñ forzh piv forzh penaos.";i['invitedToXByY']=s("Pedet da gemer perzh e %1$s gant %2$s");i['inviteTheStudentBack']="Pediñ ar studier da zistreiñ";i['lastActiveDate']="Oberiant";i['lichessClasses']="Klasoù echedoù";i['lichessProfileXCreatedForY']=s("Profil Lichess %1$s krouet evit %2$s.");i['lichessUsername']="Anv-implijer Lichess";i['makeSureToCopy']="Bezit sur da gopiañ pe da vizskrivañ ar ger-tremen bremañ. N\\'ho po ket tro d\\'hen ober diwezhatoc\\'h!";i['managed']="Meret";i['messageAllStudents']="Kelaouiñ an holl studierien diwar-benn an dafar kelenn nevez";i['na']="N\\'eus ket";i['nbPendingInvitations']=p({"one":"Ur bedadenn o c\\'hortoz ur respont","two":"%s bedadenn o c\\'hortoz ur respont","few":"%s pedadenn o c\\'hortoz ur respont","many":"%s pedadenn o c\\'hortoz ur respont","other":"%s pedadenn o c\\'hortoz ur respont"});i['nbStudents']=p({"one":"Studier","two":"%s studier","few":"%s studier","many":"%s studier","other":"%s studier"});i['nbTeachers']=p({"one":"Kelenner","two":"%s gelenner","few":"%s kelenner","many":"%s kelenner","other":"%s kelenner"});i['newClass']="Klas nevez";i['newsEdit1']="An holl geleier er memes lec\\'h.";i['newsEdit2']="En nec\\'h emañ ar c\\'heleier nevez. Ne ziverkit ket ar c\\'heleier kozh.";i['newsEdit3']="Rannit ar c\\'heleier gant ---\nMod-se ho po ul linenn a-blaen.";i['noClassesYet']="Klas ebet evit poent.";i['noRemovedStudents']="Studier skarzhet ebet.";i['noStudents']="Studier ebet evit poent.";i['nothingHere']="Netra amañ evit poent.";i['notifyAllStudents']="Kemenn an holl studierien";i['onlyVisibleToTeachers']="Gwelet e vo gant ar gelennerien hepken";i['overDays']="A zeiz da zeiz";i['passwordX']=s("Ger-tremen: %s");i['privateWillNeverBeShown']="Prevez. Ne vo ket skignet er-maez eus ar c\\'hlas. Sikour a ra evit derc\\'hel soñj eus piv eo ar studier.";i['progress']="Araokadur";i['quicklyGenerateSafeUsernames']="Krouiñ a ra anvioù-implijer ha gerioù-tremen suraet evit ar studierien war ar prim";i['realName']="Anv gwir";i['realUniqueEmail']="Chomlec\\'h postel nemetañ ha gwir ar studier. Ur gemennadenn gadarnaat a vo kaset gant ul liamm da glikañ warnañ evit dieubiñ ar gont.";i['release']="Dieubiñ";i['releaseDesc1']="Ur wech dieubet ur gont ne c\\'halloc\\'h ket mui bezañ mestr warni. Gallout a ray ar studier lazhañ ar mod evit ar vugale ha cheñch ar ger-tremen e-unan.";i['releaseDesc2']="Ezel eus ar c\\'hlasad e chomo ar studier ur wech dieubet e gont.";i['releaseTheAccount']="Dieubiñ kont ar studier evit ma c\\'hallfe merañ anezhi e-unan.";i['removedStudents']="Lemet";i['removeStudent']="Skarzhañ ur studier";i['reopen']="Digeriñ en-dro";i['resetPassword']="Cheñch ger-tremen";i['sendAMessage']="Kas ur gemennadenn d\\'an holl studierien.";i['studentCredentials']=s("Studier:  %1$s\nAnv-implijer: %2$s\nGer-tremen: %3$s");i['students']="Studierien";i['teachClassesOfChessStudents']="Deskit echedoù d\\'ho studierien gant Lichess.";i['teachers']="Kelennerien";i['teachersOfTheClass']="Kelennerien ar c\\'hlas";i['teachersX']=s("Kelennerien: %s");i['thisStudentAccountIsManaged']="Meret eo kont ar studier-mañ";i['timePlaying']="Amzer o c\\'hoari";i['trackStudentProgress']="Heuilhit araokadennoù ho studierien a-fet krogadoù ha poelladennoù";i['upgradeFromManaged']="Kas ur gont meret da emren";i['variantXOverLastY']=s("%1$s e-pad an/ar %2$s");i['visibleByBothStudentsAndTeachers']="A c\\'hall bezañ gwelet gant ar gelennerien hag ar studierien";i['welcomeToClass']=s("Donedigezh vat en ho klas: %s.\nAmañ emañ al liamm evit mont ennañ.");i['winrate']="Feur trec\\'h"})()