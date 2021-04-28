# ZooAppStarter
Simple program managing an app with ZooKeeper

## Usage
Execute `./gradlew run --console=plain --args="<host>:<port> <command>"` in base directory.
`<host>:<port>` is ZooKeeper server address, `<command>` is a command to be executed after znode `/z` is created.
Whenever a descendant of `/z` is created program displays total number of descendants of `/z`, and on Enter program 
displays whole `/z` tree.