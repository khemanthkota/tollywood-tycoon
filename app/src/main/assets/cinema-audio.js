/* Original synthesized score; no recordings, downloads or microphone access. */
(()=>{
'use strict';
const langs={'te-IN':'Telugu','hi-IN':'Hindi','ta-IN':'Tamil','ml-IN':'Malayalam','kn-IN':'Kannada','en-US':'English'};
const defaults={tollywood:'te-IN',bollywood:'hi-IN',kollywood:'ta-IN',mollywood:'ml-IN',sandalwood:'kn-IN',hollywood:'en-US'};
let prefs={music:false,voice:true,effects:true,musicVolume:.18,voiceVolume:.9,language:'auto',commentary:'full'};
try{const v=JSON.parse(localStorage.getItem('cinema-audio-v1'));if(v&&typeof v==='object')for(const k of Object.keys(prefs))if(typeof v[k]===typeof prefs[k])prefs[k]=v[k]}catch(_){}
for(const k of ['musicVolume','voiceVolume'])prefs[k]=Math.max(0,Math.min(1,prefs[k]));
if(!['full','essential','mute'].includes(prefs.commentary))prefs.commentary='full';
if(prefs.language!=='auto'&&!langs[prefs.language])prefs.language='auto';
let ctx,gain,timer,beat=0,unlocked=false,paused=false,ducked=false;
const panel=document.createElement('details');panel.className='cinema-audio';panel.innerHTML=`<summary>♫ Music & voice</summary><div class="audio-options"><label><input id="audioMusic" type="checkbox"> Background music</label><label><input id="audioVoice" type="checkbox"> Voice announcements</label><label><input id="audioEffects" type="checkbox"> Auction sounds</label><label>Music volume <input id="audioMusicVolume" type="range" min="0" max="1" step="0.01"></label><label>Voice volume <input id="audioVoiceVolume" type="range" min="0" max="1" step="0.01"></label><label>Auctioneer commentary <select id="audioCommentary"><option value="full">Full commentary</option><option value="essential">Essential announcements</option><option value="mute">Mute auctioneer</option></select></label><label>Voice language <select id="audioLanguage"><option value="auto">Match movie industry</option>${Object.entries(langs).map(([k,v])=>`<option value="${k}">${v}</option>`).join('')}</select></label><button type="button" id="audioTest">Test voice</button><p id="audioStatus" role="status">Enable music to start. Voice uses your phone’s text-to-speech voices.</p></div>`;
(document.querySelector('header')||document.body).appendChild(panel);
const el=id=>document.getElementById(id),status=el('audioStatus');
function save(){try{localStorage.setItem('cinema-audio-v1',JSON.stringify(prefs))}catch(_){}}
function volume(){if(gain)gain.gain.setTargetAtTime(prefs.music&&!paused?(ducked?prefs.musicVolume*.22:prefs.musicVolume):0,ctx.currentTime,.12)}
function tone(freq,start,duration,level,type='sine',bus=gain){const o=ctx.createOscillator(),g=ctx.createGain();o.type=type;o.frequency.value=freq;g.gain.setValueAtTime(0,start);g.gain.linearRampToValueAtTime(level,start+.015);g.gain.exponentialRampToValueAtTime(.0001,start+duration);o.connect(g);g.connect(bus);o.start(start);o.stop(start+duration+.02)}
function tick(){if(!ctx||paused||!prefs.music)return;const notes=[261.63,293.66,329.63,392,440,392,329.63,293.66,261.63,329.63,392,523.25,440,392,329.63,293.66],t=ctx.currentTime+.03;tone(notes[beat%16],t,.65,.16,'triangle');if(beat%4===0){tone(130.81,t,1.7,.11);tone(196,t,1.6,.06)}if(beat%2===0)tone(65.4,t,.14,.19);beat++}
function start(){if(!unlocked||paused||!prefs.music)return;try{if(!ctx){ctx=new(window.AudioContext||window.webkitAudioContext)();gain=ctx.createGain();gain.gain.value=0;gain.connect(ctx.destination)}ctx.resume();volume();if(!timer){tick();timer=setInterval(tick,420)}}catch(_){status.textContent='Music is unavailable on this device.'}}
function stop(){if(timer){clearInterval(timer);timer=null}volume()}
function unlock(){unlocked=true;start()}
document.addEventListener('pointerdown',unlock,{once:true});
for(const [key,id] of [['music','audioMusic'],['voice','audioVoice'],['effects','audioEffects']]){el(id).checked=prefs[key];el(id).addEventListener('change',()=>{prefs[key]=el(id).checked;save();if(key==='music'){unlock();if(!prefs.music)stop()}if(key==='voice'&&!prefs.voice){window.CinemaVoice?.stop();window.speechSynthesis?.cancel();ducked=false;volume()}})}
for(const [key,id] of [['musicVolume','audioMusicVolume'],['voiceVolume','audioVoiceVolume']]){el(id).value=prefs[key];el(id).addEventListener('input',()=>{prefs[key]=Number(el(id).value);save();volume()})}
el('audioCommentary').value=prefs.commentary;el('audioCommentary').addEventListener('change',()=>{prefs.commentary=el('audioCommentary').value;save();if(prefs.commentary==='mute'){window.CinemaVoice?.stop();window.speechSynthesis?.cancel();ducked=false;volume()}});
el('audioLanguage').value=prefs.language;el('audioLanguage').addEventListener('change',()=>{prefs.language=el('audioLanguage').value;save()});
function language(){return prefs.language==='auto'?(defaults[new URLSearchParams(location.search).get('industry')||'tollywood']||'en-US'):prefs.language}
function text(kind,p,n,lang){const sold=kind==='sold';const templates={
 'en-US':sold?`${p} wins for ${n} crore.`:`${p} bids ${n} crore.`,
 'te-IN':sold?`${n} కోట్లకు ${p} గెలుచుకున్నారు.`:`${p} బిడ్ ${n} కోట్లు.`,
 'hi-IN':sold?`${p} ने ${n} करोड़ में जीता।`:`${p} की बोली ${n} करोड़।`,
 'ta-IN':sold?`${n} கோடிக்கு ${p} வென்றார்.`:`${p} ஏலம் ${n} கோடி.`,
 'ml-IN':sold?`${n} കോടിക്ക് ${p} വിജയിച്ചു.`:`${p} ലേലം ${n} കോടി.`,
 'kn-IN':sold?`${n} ಕೋಟಿಗೆ ${p} ಗೆದ್ದಿದ್ದಾರೆ.`:`${p} ಬಿಡ್ ${n} ಕೋಟಿ.`};return templates[lang]||templates['en-US']}
function speak(message,lang,fallback){if(!prefs.voice||paused||prefs.commentary==='mute')return;ducked=true;volume();if(window.CinemaVoice){window.CinemaVoice.speak(message,lang,prefs.voiceVolume,fallback);return}if(window.speechSynthesis&&window.SpeechSynthesisUtterance){speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(message);u.lang=lang;u.volume=prefs.voiceVolume;u.onend=u.onerror=()=>{ducked=false;volume()};speechSynthesis.speak(u)}else{ducked=false;volume();status.textContent='Voice announcements are available in the Android app.'}}
function effect(kind){if(!prefs.effects||paused||!unlocked)return;try{if(!ctx){ctx=new(window.AudioContext||window.webkitAudioContext)();gain=ctx.createGain();gain.connect(ctx.destination);volume()}ctx.resume();const bus=ctx.createGain();bus.gain.value=.22;bus.connect(ctx.destination);const t=ctx.currentTime;if(kind==='sold'){[523.25,659.25,783.99].forEach((f,i)=>tone(f,t+i*.09,.35,.25,'triangle',bus))}else tone(880,t,.12,.2,'sine',bus);setTimeout(()=>bus.disconnect(),1000)}catch(_){}}
window.CinemaAudio={announce(kind,player,amount){unlock();effect(kind);if(prefs.commentary==='mute'||prefs.commentary==='essential'&&kind==='bid')return;const l=language();speak(text(kind,player,amount,l),l,text(kind,player,amount,'en-US'))},call(stage){if(prefs.commentary==='mute')return;const l=language(),calls={'en-US':['Going once','Going twice','Final call'],'te-IN':['మొదటి పిలుపు','రెండవ పిలుపు','చివరి పిలుపు'],'hi-IN':['पहली पुकार','दूसरी पुकार','आखिरी पुकार'],'ta-IN':['முதல் அழைப்பு','இரண்டாவது அழைப்பு','இறுதி அழைப்பு'],'ml-IN':['ആദ്യ വിളി','രണ്ടാം വിളി','അവസാന വിളി'],'kn-IN':['ಮೊದಲ ಕರೆ','ಎರಡನೇ ಕರೆ','ಕೊನೆಯ ಕರೆ']};const i=Math.max(0,Math.min(2,stage));speak((calls[l]||calls['en-US'])[i],l,calls['en-US'][i])},status(value){ducked=value==='speaking';volume();if(value!=='speaking'&&value!=='idle')status.textContent=value},pause(){paused=true;ducked=false;stop();ctx?.suspend();window.CinemaVoice?.stop();window.speechSynthesis?.cancel()},resume(){paused=document.hidden;if(!paused)start()}};
el('audioTest').addEventListener('click',()=>{unlock();const l=language();speak(text('bid','Player 1',5,l),l,text('bid','Player 1',5,'en-US'))});
document.addEventListener('visibilitychange',()=>{if(document.hidden)window.CinemaAudio.pause();else window.CinemaAudio.resume()});window.addEventListener('pagehide',()=>window.CinemaAudio.pause());
})();
