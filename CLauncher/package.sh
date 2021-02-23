#! /bin/bash

flavors="Ace Condor"

function generate_params() {
    params=""
    for flavor in $flavors; do
        if [ "$3" != "" ]; then
            params="$params assemble$flavor"Release
        fi
        if [ "$1" != "" ]; then
            params="$params assemble$flavor$1"Release
        fi
        if [ "$2" != "" ] && [ "$3" != "" ]; then
            params="$params assemble$flavor$2"Release
        fi
        if [ "$1" != "" ] && [ "$2" != "" ]; then
            if [ "$1" == "Quickstep" ]; then
                params="$params assemble$flavor$1$2"Release
            else
                params="$params assemble$flavor$2$1"Release
            fi
        fi
    done
    echo $params
}

last_commit_id=`git log | grep "^commit"  | sed -n '1p'| awk '{print "'\''"$2"'\''"}'`
old_commit_id=`grep "ext.last_commit_id" build.gradle | awk '{print $3}'`
old_version_code=`grep versionCode build.gradle | awk '{printf $2}'`
old_version_name=`grep versionName build.gradle | awk '{printf $2}'`

count=${old_version_code: -3}
count=`expr $count + 1`
version_code="2128"$count
version_name="2.128"$count

# modify version code and version name
if [ "$last_commit_id" != "$old_commit_id" ]; then
    sed -i "s/versionCode .*/versionCode $version_code/g" build.gradle
    sed -i "s/versionName .*/versionName \"$version_name\"/g" build.gradle
    sed -i "s/last_commit_id .*/last_commit_id = $last_commit_id/g" build.gradle
fi

if [ $# -eq 0 ]; then
    arg1="Quickstep"
    arg2="Go"
    arg3="Aosp"
else
    arg1=$1
    arg2=$2
    arg3=$3
fi

# build projects
chmod +x gradlew
./gradlew clean
params=$(generate_params $arg1 $arg2 $arg3)
./gradlew $params

# copy apk files
rm -rf output
mkdir output
echo "[">output/output.json
for param in $params; do
    flavor=${param##assemble}
    flavor=${flavor%%Release}
    flavor=${flavor,}
	cp build/outputs/apk/$flavor/release/*.apk output/
	cat build/outputs/apk/$flavor/release/output.json>>output/output.json
	echo ",">>output/output.json
done
echo "]">>output/output.json

# restore version code and version name
#sed -i "s/versionCode .*/versionCode $old_version_code/g" build.gradle
#sed -i "s/versionName .*/versionName $old_version_name/g" build.gradle
