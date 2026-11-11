"""Compile the real Desktop transport with a synthetic HTTP client binding."""
import argparse
import os
import pathlib
import subprocess

p = argparse.ArgumentParser()
p.add_argument('--toolchain', required=True)
p.add_argument('--app', required=True)
a = p.parse_args()
root = pathlib.Path(__file__).resolve().parents[2]
tool = pathlib.Path(a.toolchain)
lib = tool / 'kotlin2421/kotlinc/lib'
java = next((tool / 'jre21').glob('*/bin/java.exe'))
out = root / '.local/transport-test'
out.mkdir(parents=True, exist_ok=True)
source = root / 'composeApp/src/desktopMain/kotlin/com/nuvio/app/features/player/AddonPlayerEventTransport.desktop.kt'
(out / 'Transport.kt').write_text(source.read_text().replace('actual ', ''), encoding='utf-8')
(out / 'Binding.kt').write_text('package com.nuvio.app.features.addons\ninternal val desktopHttpClient = okhttp3.OkHttpClient()\n', encoding='utf-8')
(out / 'Probe.kt').write_text('''package com.nuvio.app.features.player
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*

object TransportProbe {
 @JvmStatic fun main(args:Array<String>) = runBlocking {
  val hits = AtomicInteger()
  val started = CompletableDeferred<Unit>()
  val executor = Executors.newCachedThreadPool()
  val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  server.executor = executor
  server.createContext("/ok") { exchange ->
   check(exchange.requestHeaders.getFirst("User-Agent") == "libmpv")
   check(exchange.requestHeaders.getFirst("Cache-Control") == "no-store")
   exchange.sendResponseHeaders(204,-1); exchange.close()
  }
  server.createContext("/redirect") { exchange ->
   exchange.responseHeaders.add("Location","/forbidden")
   exchange.sendResponseHeaders(302,-1); exchange.close()
  }
  server.createContext("/forbidden") { exchange -> hits.incrementAndGet(); exchange.sendResponseHeaders(204,-1); exchange.close() }
  server.createContext("/slow") { exchange ->
   started.complete(Unit)
   try { Thread.sleep(3000); exchange.sendResponseHeaders(204,-1) } catch (_:Exception) {} finally { exchange.close() }
  }
  server.start()
  val base = "http://127.0.0.1:${server.address.port}"
  try {
   AddonPlayerEventTransport.send("$base/ok")
   check(runCatching { AddonPlayerEventTransport.send("$base/redirect") }.isFailure)
   check(hits.get()==0)
   val before=System.nanoTime()
   check(runCatching { AddonPlayerEventTransport.send("$base/slow") }.isFailure)
   check((System.nanoTime()-before)/1_000_000 < 2800)
   val job=launch { AddonPlayerEventTransport.send("$base/slow") }
   delay(100)
   val cancelAt=System.nanoTime(); job.cancelAndJoin()
   check((System.nanoTime()-cancelAt)/1_000_000 < 500)
   println("Transport passed: empty 204, matching UA, no redirects, two-second timeout, prompt cancellation; loopback only")
  } finally {
   server.stop(0); executor.shutdownNow()
   com.nuvio.app.features.addons.desktopHttpClient.dispatcher.executorService.shutdownNow()
   com.nuvio.app.features.addons.desktopHttpClient.connectionPool.evictAll()
  }
 }
}
''', encoding='utf-8')
cp = os.pathsep.join(map(str, pathlib.Path(a.app).glob('*.jar')))
command = [str(java), '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-jvm-target', '17', '-classpath', cp, '-d', str(out / 'compiled'), *map(str, out.glob('*.kt'))]
subprocess.run(command, check=True)
subprocess.run([str(java), '-cp', str(out / 'compiled') + os.pathsep + cp, 'com.nuvio.app.features.player.TransportProbe'], check=True)
