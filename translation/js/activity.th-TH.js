"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.activity)window.i18n.activity={};let i=window.i18n.activity;i['activity']="กิจกรรม";i['competedInNbSwissTournaments']=p({"other":"เล่นเกม %s เสร็จในทวร์นาเมนต์สวิส"});i['competedInNbTournaments']=p({"other":"ได้แข่งขันใน %s ทัวร์นาเมนต์"});i['completedNbGames']=p({"other":"ได้เล่นเกมแบบยาวนานสมบูรณ์ %s เกม"});i['completedNbVariantGames']=p({"other":"ได้เล่นเกมแบบยาวนานสมบูรณ์ %1$s %2$s เกม"});i['createdNbStudies']=p({"other":"ได้สร้าง %s การศึกษาใหม่"});i['followedNbPlayers']=p({"other":"ได้เริ่มติดตาม %s ผู้เล่น"});i['gainedNbFollowers']=p({"other":"ได้ผู้ติดตามใหม่ %s ราย"});i['hostedALiveStream']="ได้เป็นเจ้าภาพสตรีมสด";i['hostedNbSimuls']=p({"other":"ได้เป็นโฮสต์ %s มหกรรมไซมัลเทเนียส"});i['inNbCorrespondenceGames']=p({"other":"ใน %1$s เกมแบบยาวนาน"});i['joinedNbSimuls']=p({"other":"ได้มีส่วนร่วมใน %s มหกรรมไซมัลเทเนียส"});i['joinedNbTeams']=p({"other":"ได้เข้าร่วม %s ทีม"});i['playedNbGames']=p({"other":"ได้เล่น %2$s %1$s เกม"});i['playedNbMoves']=p({"other":"ได้เล่น %1$s ตาเดิน"});i['postedNbMessages']=p({"other":"ได้โพสต์ %1$s ข้อความใน %2$s"});i['practicedNbPositions']=p({"other":"ได้ฝึกหัด %1$s ตำแหน่ง ใน %2$s"});i['rankedInSwissTournament']=s("ลำดับ# %1$s ใน %2$s");i['rankedInTournament']=p({"other":"อันดับ #%1$s (ท็อป %2$s%%) กับ %3$s เกมใน %4$s"});i['signedUp']="ได้ลงทะเบียนที่ lichess";i['solvedNbPuzzles']=p({"other":"ได้เล่นแก้ %s ปริศนากลยุทธ์"});i['supportedNbMonths']=p({"other":"สนับสนุน lichess.org เป็นเวลา %1$s เดือน โดยเป็น %2$s"})})()