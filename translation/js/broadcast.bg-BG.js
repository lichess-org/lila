"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['addRound']="Добави рунд";i['broadcastCalendar']="Календар на излъчванията";i['broadcasts']="Излъчване";i['completed']="Завършени";i['credits']="Признателност на източника";i['currentGameUrl']="URL на настоящата партия";i['definitivelyDeleteRound']="Окончателно изтрийте този рунд и всичките му игри.";i['definitivelyDeleteTournament']="Окончателно изтрий целия турнир, всичките му рундове и игри.";i['deleteAllGamesOfThisRound']="Изтрийте този рунд и всичките му игри. Източникът трябва да е активен за да можете да ги възстановите.";i['deleteRound']="Изтрий този рунд";i['deleteTournament']="Изтрий този турнир";i['downloadAllRounds']="Изтегли всички рундове";i['federation']="Федерация";i['fideFederations']="ФИДЕ федерации";i['fideProfile']="ФИДЕ профил";i['fullDescription']="Пълно описание на събитието";i['fullDescriptionHelp']=s("Незадължително дълго описание на излъчването. %1$s е налично. Дължината трябва да по-малка от %2$s знака.");i['liveBroadcasts']="Излъчвания на турнир на живо";i['myBroadcasts']="Моите излъчвания";i['nbBroadcasts']=p({"one":"%s излъчване","other":"%s излъчвания"});i['newBroadcast']="Нови предавания на живо";i['ongoing']="Текущи";i['replacePlayerTags']="По избор: промени имената на играчите, рейтингите и титлите";i['resetRound']="Нулирай този рунд";i['roundName']="Име на рунда";i['roundNumber']="Номер на рунда";i['sourceUrlHelp']="Уебадресът, който Lichess ще проверява, за да получи осъвременявания на PGN. Той трябва да е публично достъпен от интернет.";i['startDateHelp']="По избор, ако знаете, кога започва събитието";i['subscribedBroadcasts']="Излчвания които следя";i['tournamentDescription']="Кратко описание на турнира";i['tournamentName']="Име на турнира";i['upcoming']="Предстоящи"})()