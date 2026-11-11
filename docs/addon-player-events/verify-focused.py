import pathlib,subprocess,os
import argparse
p=argparse.ArgumentParser()
p.add_argument('--toolchain',required=True,help='Existing directory with kotlin2421/kotlinc and jre21')
p.add_argument('--app',required=True,help='Installed Nuvio app jar directory')
a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2]; tool=pathlib.Path(a.toolchain);lib=tool/'kotlin2421/kotlinc/lib';java=next((tool/'jre21').glob('*/bin/java.exe'))
(root/'.local').mkdir(exist_ok=True)

app=pathlib.Path(a.app)
cp=os.pathsep.join(map(str,[*app.glob('*.jar'),tool/'junit-4.13.2.jar',tool/'hamcrest-core-1.3.jar',lib/'kotlin-test.jar',lib/'kotlin-test-junit.jar']))
sources=[*root.glob('composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/AddonPlayerEvent*.kt'),root/'composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonTransportUrls.kt',root/'composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/AddonPlayerEventsTest.kt']
args=[str(java),'-cp',str(lib/'*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-Xplugin='+str(lib/'compose-compiler-plugin.jar'),'-classpath',cp,'-d',str(root/'.local/compiled'),*map(str,sources)]
result=subprocess.run(args,capture_output=True,text=True);print(result.stdout,result.stderr);
if result.returncode: raise SystemExit(result.returncode)
result=subprocess.run([str(java),'-cp',str(root/'.local/compiled')+os.pathsep+cp,'org.junit.runner.JUnitCore','com.nuvio.app.features.player.AddonPlayerEventsTest'],capture_output=True,text=True);print(result.stdout,result.stderr);
if result.returncode: raise SystemExit(result.returncode)
