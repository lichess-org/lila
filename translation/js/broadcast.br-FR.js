"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.broadcast)window.i18n.broadcast={};let i=window.i18n.broadcast;i['broadcasts']="War-eeun";i['completed']="Tremenet";i['credits']="Orin ar vammenn";i['definitivelyDeleteTournament']="Dilemel an tournamant da viken, an holl grogadoù ha pep tra penn-da-benn.";i['deleteTournament']="Dilemel an tournamant-mañ";i['fullDescription']="Deskrivadur an abadenn a-bezh";i['fullDescriptionHelp']=s("Deskrivadur hir ar skignañ war-eeun ma fell deoc\\'h.%1$s zo dijabl. Ne vo ket hiroc\\'h evit %2$s sin.");i['liveBroadcasts']="Tournamantoù skignet war-eeun";i['newBroadcast']="Skignañ war-eeun nevez";i['ongoing']="O ren";i['roundNumber']="Niverenn ar batalm";i['sourceUrlHelp']="An URL a ray Lichess ganti evit kaout hizivadurioù ar PGN. Ret eo dezhi bezañ digor d\\'an holl war Internet.";i['startDateHelp']="Diret eo, ma ouzit pegoulz e krogo";i['upcoming']="A-benn nebeut"})()