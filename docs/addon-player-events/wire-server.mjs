import http from 'node:http';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';
import path from 'node:path';
const {PlaybackPositions,parsePlayerEvent,playbackNoticePlan,insertPositionNotice}=await import(pathToFileURL(path.join(process.argv[2],'src/playback-notice.js')));
let now=1000;
const positions=new PlaybackPositions({now:()=>now});
const server=http.createServer((req,res)=>{
 const agent=req.headers['user-agent'];
 const path=req.url.replace('/c/synthetic','');
 if(path.startsWith('/player/')){
  const event=parsePlayerEvent(path);
  if(!event){res.writeHead(400).end();return;}
  positions.update('synthetic','loopback',agent,event);
  res.end('{}');return;
 }
 now+=12000;
 const plan=playbackNoticePlan(positions.position('synthetic','loopback','series','tt1234567:4:17',{agent}));
 const text=plan?insertPositionNotice('1\n00:00:00,000 --> 00:20:00,000\nSynthetic dialogue\n','USS fixture',plan):'';
 res.setHeader('Content-Type','application/json');res.end(JSON.stringify({plan,text}));
});
server.listen(0,'127.0.0.1',()=>{console.log(JSON.stringify({port:server.address().port}));});
