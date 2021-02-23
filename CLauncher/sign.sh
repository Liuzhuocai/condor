#! /bin/bash -x
substring='.apk'
replacement='_sign.apk'
txt=$(find build/ -name *.apk)
for i in `echo $txt | xargs`
do
  java -Xmx2048m -Djava.library.path="/home/condor/workspace/key/linux-x86/lib64" -jar "/home/condor/workspace/key/linux-x86/framework/signapk.jar" '/home/condor/workspace/key/condorKey_13/platform.x509.pem' '/home/condor/workspace/key/condorKey_13/platform.pk8' $i ${i/%$substring/$replacement};
  #java -Xmx2048m -Djava.library.path="/home/liuzuo/ext/googleSource/out/host/linux-x86/lib64" -jar "/home/liuzuo/ext/googleSource/out/host/linux-x86/framework/signapk.jar" '/home/liuzuo/ext/key/condorKey_13/platform.x509.pem' '/home/liuzuo/ext/key/condorKey_13/platform.pk8' $i ${i/%$substring/$replacement};
 done




