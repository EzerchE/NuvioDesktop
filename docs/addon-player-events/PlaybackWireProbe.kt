package com.nuvio.app.features.player

import java.net.URI
import kotlinx.serialization.json.*

object PlaybackWireProbe {
 @JvmStatic fun main(args:Array<String>){
  val base="http://127.0.0.1:${args[0]}"
  fun get(url:String):String {
   val connection=URI(url).toURL().openConnection()
   connection.setRequestProperty("User-Agent","libmpv")
   connection.connectTimeout=2000;connection.readTimeout=2000
   return connection.getInputStream().bufferedReader().use{it.readText()}
  }
  val policy=AddonPlayerEventPolicy()
  val start=policy.update("file-a","series","tt1234567:4:17",185000,1200000,true,false,false,true,1f,0).single()
  get(start.url("$base/c/synthetic/manifest.json"))
  val first=Json.parseToJsonElement(get("$base/subtitle.srt")).jsonObject
  check(first["plan"]!!.jsonObject["startSeconds"]!!.jsonPrimitive.double==198.5)
  check(first["plan"]!!.jsonObject["durationSeconds"]!!.jsonPrimitive.int==10)
  check(first["text"]!!.jsonPrimitive.content.contains("USS fixture\nSynthetic dialogue"))
  val pause=policy.update("file-a","series","tt1234567:4:17",600000,1200000,false,false,false,true,1f,12000).single()
  get(pause.url("$base/c/synthetic/manifest.json"))
  val paused=Json.parseToJsonElement(get("$base/subtitle.srt")).jsonObject
  check(paused["plan"]!!.jsonObject["startSeconds"]!!.jsonPrimitive.double==601.5)
  get(policy.stop()!!.url("$base/c/synthetic/manifest.json"))
  val stopped=Json.parseToJsonElement(get("$base/subtitle.srt")).jsonObject
  check(stopped["plan"]==JsonNull)
  println("Wire acceptance passed: resume + simulated 12-second preparation, paused seek, ten-second compound SRT, stop clearing; no external calls")
 }
}
