"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['boardPlay']="Main game dengan API papan";i['botPlay']="Main game dengan API bot";i['carefullySelect']="Hati-hati dalam memilih apa yang Anda perbolehkan.";i['challengeBulk']="Membuat banyak permainan sekaligus untuk pemain lain";i['challengeRead']="Baca tantangan masuk";i['challengeWrite']="Kirim, terima dan tolak tantangan";i['copyTokenNow']="Pastikan untuk menyalin atau menuliskan token sekarang. Anda tidak akan bisa melihatnya lagi!";i['doNotShareIt']="Token ini akan memberikan akses ke akun Anda. JANGAN membagikan ini ke siapapun!";i['emailRead']="Baca alamat email";i['engineRead']="Lihat dan gunakan komputer eksternal";i['engineWrite']="Buat dan perbarui komputer eksternal";i['followRead']="Baca pemain yang diikuti";i['followWrite']="Ikut dan batal ikut pemain lain";i['msgWrite']="Kirim pesan pribadi ke pemain lain";i['newAccessToken']="Token akses API pribadi baru";i['personalAccessTokens']="Token Akses API Pribadi";i['preferenceRead']="Preferensi baca";i['preferenceWrite']="Preferensi tulis";i['puzzleRead']="Baca aktivitas puzzle";i['racerWrite']="Buat dan ikut balap puzzle";i['rememberTokenUse']="Supaya Anda ingat token ini untuk apa";i['studyRead']="Baca studi privat dan siaran langsung";i['studyWrite']="Buat, perbarui, hapus studi dan siaran langsung";i['teamLead']="Kelola tim yang dipimpin: kirim pesan, tendang anggota";i['teamRead']="Baca informasi tim privat";i['teamWrite']="Bergabung dan keluar tim";i['tokenDescription']="Deskripsi token";i['tokenGrantsPermission']="Token memberikan orang lain izin untuk menggunakan akun Anda.";i['tournamentWrite']="Buat, perbarui, dan ikut turnamen";i['webLogin']="Buat sesi website terautentikasi (memberikan akses penuh!)";i['webMod']="Menggunakan alat moderator (dalam batasan izin Anda)";i['whatTheTokenCanDo']="Apa yang token ini dapat lakukan:"})()