"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.swiss)window.i18n.swiss={};let i=window.i18n.swiss;i['dutchSystem']="Doare an Izelvroioù";i['FIDEHandbook']="Dornlevr ar FIDE";i['forbiddenPairings']="Koubladennoù difennet";i['forbiddenPairingsHelp']="Anvioù implijer d\\'ar re ne rankont ket c\\'hoari an eil re a-enep d\\'ar re all (familh, ha kement-zo). Daou anv implijer dre linenn, rannet gant un esaouenn (ur spas).";i['identicalForbidden']="Difennet";i['lateJoin']="Kemer perzh gant dale";i['newSwiss']="Tournamant suis nevez";i['numberOfGames']="Niver a grogadoù";i['ongoingGames']=p({"one":"Ur c\\'hrogad o ren","two":"Daou grogad o ren","few":"Krogad o ren","many":"Krogad o ren","other":"Krogad o ren"});i['pairingsQ']="Penaos e vez koublet ar c\\'hoarierien?";i['pause']="Ehan";i['playYourGames']="C\\'hoariit ho krogadoù";i['pointsCalculationA']="Ur poent evit an trec\\'h, un hanter hini evit bezañ rampo hag hini ebet ma kollit.\nMa n\\'eo ket bet kavet un enebour deoc\\'h e-pad ur rondenn ho po ur \\'bye\\' hag a dalv ur poent.";i['pointsCalculationQ']="Penaos e vez jedet ar poentoù?";i['similarToOTB']="Damheñvel ouzh an tournamantoù ezlinenn (OTB)";i['sonnebornBergerScore']="Skor Sonneborn–Berger";i['startingIn']="Kregiñ a ray a-benn";i['swissTournaments']="Tournamantoù suis";i['swissVsArenaA']="An holl re a gemer perzh en un tournamant suis a c\\'hoario bep a grogad a-enep ar re all. Interesant eo evit ar c\\'hluboù hag an tournamantoù ofisiel.";i['tournDuration']="Padelezh an tournamant";i['tournStartDate']="Deiziad an tournamant";i['unlimitedAndFree']="Didermen ha digoust"})()