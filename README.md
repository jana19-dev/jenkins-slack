<img src="media/logo.png" alt="logo" width="100px"/>

# Jenkins Slack Notification
Send a slack notification for the current build

Usage:

`notifySlack status: currentBuild.currentResult, message: errorMessage, channel: '#builds'`
```
  status = 'STARTED' or 'SUCCESS' or 'FAILURE'
  message = Any custom message you want to display in `details` section
  channel = Slack channel to post: (eg) #builds
```

## Build Started
![build-started](media/01.gif)

## Build Passed
![build-pass](media/02.gif)

## Build Failed (Failing Tests)
![build-fail-tests](media/05.gif)

## Build Failed (Not Enough Test Coverage)
![build-fail-tests](media/03.gif)

## Build Failed (Error in Pipeline)
![build-fail-tests](media/04.gif)