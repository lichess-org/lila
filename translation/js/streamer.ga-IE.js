"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Sruthaithe uile";i['approved']="Tá do shruthlú ceadaithe.";i['becomeStreamer']="Bí i do shruthú Lichess";i['changePicture']="Athraigh/scrios do phictiúr";i['currentlyStreaming']=s("Ag sruthú faoi láthair: %s");i['downloadKit']="Íoslódáil trealamh sruthú";i['doYouHaveStream']="An bhfuil cuntas Twitch nó YouTube agat?";i['editPage']="Cuir leathanach sruthaithe in eagar";i['headline']="Ceannlíne";i['hereWeGo']="Réidh téigh!";i['keepItShort']=p({"one":"Coinnigh gearr é: %s carachtar uas","two":"Coinnigh gearr é: %s charachtar uas","few":"Coinnigh gearr é: %s gcarachtar uas","many":"Coinnigh gearr é: %s carachtar uas","other":"Coinnigh gearr é: %s carachtar uas"});i['lastStream']=s("Sruth deireanach %s");i['lichessStreamer']="Sruthú Lichess";i['lichessStreamers']="Sruthaithe Lichess";i['live']="BEO!";i['longDescription']="Cur síos fada";i['maxSize']=s("Uasmhéid: %s");i['offline']="AS LÍNE";i['optionalOrEmpty']="Roghnach. Fág folamh mura bhfuil ceann ann";i['pendingReview']="Tá modhnóirí ag athbhreithniú ar do shruth.";i['perk1']="Faigh deilbhín sraoilleán lasrach ar do phróifíl Lichess.";i['perk2']="Teigh suas go barr an liosta sruthlaithe.";i['perk3']="Cuir in iúl do do leanúna Lichess.";i['perk4']="Taispeáin do shruth i do chluichí, comórtais agus staidéir.";i['perks']="Buntáistí a bhaineann le sruthú leis an eochairfhocal";i['pleaseFillIn']="Líon isteach d’eolais sruthaithe le do thoil, agus uaslódáil pictiúr.";i['requestReview']="iarr ar athbhreithniú modhnóra";i['rule1']="Cuir an eochairfhocal \\\"lichess.org\\\" san áireamh i do theideal srutha agus bain úsáid as an gcatagóir \\\"Ficheall\\\" nuair a dhéanann tú sruth ar Lichess.";i['rule2']="Bain an eochair-fhocal nuair atá tú ag sruthlú stuif neamh-Lichess.";i['rule3']="Braithfidh Lichess do shruth go huathoibríoch agus ligfidh sé na buntáistí seo a leanas:";i['rule4']=s("Léigh ár %s chun cothrom na Féinne a chinntiú do gach duine le linn do shruth.");i['rules']="Rialacha sruthlú";i['streamerLanguageSettings']="Díríonn leathanach sruthlóra Lichess ar do lucht féachana leis an teanga a sholáthraíonn d’ardán sruthú. Socraigh an teanga réamhshocraithe cheart do do shruthanna fichille san aip nó seirbhís a úsáideann tú chun craoladh.";i['streamerName']="D\\'ainm sruthú ar Lichess";i['streamingFairplayFAQ']="cCanna maidir le cothrom na Féinne a shruthlú";i['tellUsAboutTheStream']="Inis dúinn faoi do shruth in aon abairt amháin";i['twitchUsername']="D\\'ainm úsáideora nó URL Twitch";i['uploadPicture']="Uaslódáil pictiúr";i['visibility']="Le feiceáil ar an leathanach sruthú";i['whenApproved']="Nuair atá siad faofa ó modhnóirí";i['whenReady']=s("Nuair atá tú réidh le liostáil mar sruthú Lichess, %s");i['xIsStreaming']=s("%s ag sruthlú");i['xStreamerPicture']=s("%s pictiúr sruthaithe");i['yourPage']="Do leathanach sruthú"})()