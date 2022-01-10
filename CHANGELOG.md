
<a name="v2022-01-10"></a>
## [v2022-01-10](https://github.com/FrankS77/PreProxyFS/compare/2021-12-30...v2022-01-10)

> 2022-01-10

### Chore

* Updating changelog with git-chglog

### Fix

* Add keep alive and remove timeout for a more stable connection


<a name="2021-12-30"></a>
## [2021-12-30](https://github.com/FrankS77/PreProxyFS/compare/v2021-12-30...2021-12-30)

> 2021-12-30


<a name="v2021-12-30"></a>
## [v2021-12-30](https://github.com/FrankS77/PreProxyFS/compare/v2021-01.29...v2021-12-30)

> 2021-12-30

### Chore

* Updating Gradle to 7.2
* Updating GraalVM to 21.3.0.java11
* Updating grgit, spotless, sonarlint and add awaitility dependency

### Feat

* Add a fixed connection timeout for socket connections and add a retry mechanism

### Fix

* do not open a new connection if it is already exists
* Fix spotbugs issues with getBytes (internationalization)

### Refactor

* Move unit tests to same package as main code

### Style

* check commit messages
* use spotless for formatting java files
* add spotless for Java formatting
* changelog for last versions, generated with git-chglog
* changelog for last versions, generated with git-chglog
* mark as property text

### Test

* Change Thread.sleep to await
* enable tests
* prevent non-functional code from being checked in
* more unit tests
* more unit tests
* more unit tests
* more unit tests
* more unit tests


<a name="v2021-01.29"></a>
## [v2021-01.29](https://github.com/FrankS77/PreProxyFS/compare/2021-01-29...v2021-01.29)

> 2021-01-29


<a name="2021-01-29"></a>
## [2021-01-29](https://github.com/FrankS77/PreProxyFS/compare/2021-01-23...2021-01-29)

> 2021-01-29

### Feat

* Check real time if proxies in PAC are reachable. If not use direct connection. Useful when using e.g. a VPN

### Fix

* remove not needed files for windows native distribution
* Fix spotbugs issues with getBytes (internationalization)


<a name="2021-01-23"></a>
## [2021-01-23](https://github.com/FrankS77/PreProxyFS/compare/v2021-01-23...2021-01-23)

> 2021-01-23


<a name="v2021-01-23"></a>
## [v2021-01-23](https://github.com/FrankS77/PreProxyFS/compare/2021-01-21...v2021-01-23)

> 2021-01-23

### Feat

* GitHub workflow for native image for macOS, Linux,

### Fix

* native distribution test6
* native distribution test5
* native distribution test4
* native distribution test3
* native distribution test2
* native distribution test
* Set ACTIONS_ALLOW_UNSECURE_COMMANDS to true because setup-graalvm use this deprecated command
* checkout is missing


<a name="2021-01-21"></a>
## 2021-01-21

> 2021-01-21

### Chore

* github action set-env is deprecated
* new getVersion task
* new getVersion task
* github initial test

### Docs

* use http example as description

### Feat

* initial version

### Style

* fix typos and indention

