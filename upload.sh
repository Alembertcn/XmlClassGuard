toLocal=${1:-false}
if [ $toLocal == "local" ]; then
   echo "start publishToMavenLocal"
    ./gradlew :plugin:publishToMavenLocal --stacktrace
else
   echo "start publish"
   ./gradlew :plugin:publish --stacktrace
fi
