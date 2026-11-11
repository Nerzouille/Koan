// Localhost-only WebAuthn verifier for the androidTest credential provider. No real account.
import http from 'node:http';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';

const origin = 'http://localhost:8786';
let state;
const reset = () => state = {phase:'ready',passed:false,error:'',challenge:crypto.randomBytes(32).toString('base64url')};
reset();
const decode = s => Buffer.from(s,'base64url');
function cbor(data, offset=0) {
  const h=data[offset++], major=h>>5;
  let n=h&31;
  if(n===24)n=data[offset++];
  else if(n===25){n=data.readUInt16BE(offset);offset+=2;}
  if(major===0)return[n,offset];
  if(major===1)return[-1-n,offset];
  if(major===2||major===3)return[major===2?data.subarray(offset,offset+n):data.toString('utf8',offset,offset+n),offset+n];
  if(major===5){const map=new Map();for(let i=0;i<n;i++){let k,v;[k,offset]=cbor(data,offset);[v,offset]=cbor(data,offset);map.set(k,v);}return[map,offset];}
  throw Error('Unsupported CBOR');
}
function client(response,type) {
  const raw=decode(response.clientDataJSON), data=JSON.parse(raw);
  assert.equal(data.type,type);assert.equal(data.origin,origin);assert.equal(data.challenge,state.challenge);
  assert.equal(data.crossOrigin,false);
  return raw;
}
function authenticator(raw) {
  assert.deepEqual(raw.subarray(0,32),crypto.createHash('sha256').update('localhost').digest());
  assert.equal(raw[32]&5,5,'User presence/verification flags');
}

const html=`<!doctype html><meta name="viewport" content="width=device-width"><title>Koan passkey fixture</title>
<button id="run">Run passkey check</button><pre id="out"></pre><script>
const enc=b=>btoa(String.fromCharCode(...new Uint8Array(b))).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
const dec=s=>Uint8Array.from(atob(s.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0));
const send=(path,data)=>fetch(path,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(data)}).then(async r=>{if(!r.ok)throw Error(await r.text());return r.json()});
const serialize=c=>({id:c.id,rawId:enc(c.rawId),type:c.type,response:Object.fromEntries(['clientDataJSON','attestationObject','authenticatorData','signature','userHandle'].filter(k=>c.response[k]!=null).map(k=>[k,enc(c.response[k])]))});
document.getElementById('run').onclick=async()=>{document.getElementById('run').disabled=true;try{
 if(!await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable())throw Error('Platform capability probe returned false');
 const options=await fetch('/options').then(r=>r.json());
 const creation={rp:{id:'localhost',name:'Koan test RP'},user:{id:new TextEncoder().encode('test-user'),name:'test-user',displayName:'Test User'},challenge:dec(options.challenge),pubKeyCredParams:[{type:'public-key',alg:-7}],authenticatorSelection:{residentKey:'required',userVerification:'required'},timeout:30000,attestation:'none'};
 await send('/phase',{phase:'create'});
 const credential=await navigator.credentials.create({publicKey:creation});
 await send('/register',serialize(credential));
 await send('/phase',{phase:'get'});
 const assertion=await navigator.credentials.get({publicKey:{rpId:'localhost',challenge:dec(options.challenge),allowCredentials:[{type:'public-key',id:credential.rawId}],userVerification:'required',timeout:30000}});
 await send('/assert',serialize(assertion));
 await send('/phase',{phase:'cancel'});
 let rejected=false;try{await navigator.credentials.create({publicKey:creation})}catch(e){rejected=e.name==='AbortError'||e.name==='NotAllowedError'}
 if(!rejected)throw Error('Cancel did not reject the request');
 await send('/phase',{phase:'unavailable'});
 const started=performance.now();let unavailable=false;
 try{await navigator.credentials.create({publicKey:{...creation,user:{...creation.user,name:'no-options'}}})}catch(e){unavailable=true}
 if(!unavailable||performance.now()-started>10000)throw Error('Unavailable provider did not fail promptly');
 await send('/done',{});document.getElementById('out').textContent='Passed';
}catch(e){document.getElementById('out').textContent=e.stack;await send('/error',{error:String(e)})}};
</script>`;

const fillHtml=`<!doctype html><meta name="viewport" content="width=device-width"><title>Koan autofill fixture</title>
<form><label>Username <input id="username" name="username" autocomplete="username"></label><br>
<label>Password <input id="password" name="password" type="password" autocomplete="current-password"></label></form>
<button id="focus">Focus username</button><button id="verify">Check password fill</button><pre id="out"></pre>
<script>document.getElementById('focus').onclick=()=>document.getElementById('username').focus();document.getElementById('verify').onclick=async()=>{const r=await fetch('/fill',{method:'POST',body:JSON.stringify({username:document.getElementById('username').value,password:document.getElementById('password').value})});document.getElementById('out').textContent=await r.text()};</script>`;

const server=http.createServer(async(req,res)=>{
  res.setHeader('cache-control','no-store');
  try {
    const path=req.url;
    if(req.method==='GET'){
      if(path==='/reset')reset();
      if(path==='/'){res.setHeader('content-type','text/html');res.end(html);return;}
      if(path==='/autofill'){res.setHeader('content-type','text/html');res.end(fillHtml);return;}
      res.setHeader('content-type','application/json');
      res.end(JSON.stringify(path==='/options'?{challenge:state.challenge}:{phase:state.phase,passed:state.passed,error:state.error}));return;
    }
    let raw='';for await(const chunk of req)raw+=chunk;
    const data=JSON.parse(raw||'{}');
    if(path==='/phase')state.phase=data.phase;
    if(path==='/error')state.error=data.error;
    if(path==='/fill'){assert.equal(data.username,'test-user');assert.equal(data.password,'test-password');state.passed=true;console.log('PASS: system username/password Autofill');}
    if(path==='/register'){
      client(data.response,'webauthn.create');
      const [att]=cbor(decode(data.response.attestationObject)), auth=att.get('authData');
      assert.equal(att.get('fmt'),'none');authenticator(auth);assert.equal(auth[32]&64,64);
      const length=auth.readUInt16BE(53);state.id=auth.subarray(55,55+length).toString('base64url');
      assert.equal(state.id,data.id);assert.equal(state.id,data.rawId);
      const [key]=cbor(auth,55+length);assert.equal(key.get(3),-7);
      state.key=crypto.createPublicKey({key:{kty:'EC',crv:'P-256',x:key.get(-2).toString('base64url'),y:key.get(-3).toString('base64url')},format:'jwk'});
    }
    if(path==='/assert'){
      const clientJSON=client(data.response,'webauthn.get'),auth=decode(data.response.authenticatorData);
      authenticator(auth);assert.equal(data.id,state.id);assert.equal(data.rawId,state.id);
      assert(crypto.verify('sha256',Buffer.concat([auth,crypto.createHash('sha256').update(clientJSON).digest()]),state.key,decode(data.response.signature)),'P-256 signature verification failed');
      state.signatureVerified=true;
    }
    if(path==='/done'){assert(state.signatureVerified);state.passed=true;state.phase='done';console.log('PASS: capability, registration, signature and cancellation');}
    res.setHeader('content-type','application/json');res.end('{}');
  }catch(error){state.error=String(error);console.error(state.error);res.statusCode=400;res.end(state.error);}
});
server.listen(8786,'127.0.0.1',()=>console.log('Local passkey fixture on 127.0.0.1:8786'));
