<img src="media/logo.png" alt="logo" width="100px"/>

# Jenkins Slack Notification
Send a slack notification for the current build

Usage:
```
@Library('slack-notify') _ // import global shared library

notifySlack status: currentBuild.currentResult, message: errorMessage, channel: '#builds'
```
```
  message = (Optional) Any custom message you want to display in `details` section
  status = (Optional - defaults to 'STARTED') 'STARTED' or 'SUCCESS' or 'UNSTABLE' or 'FAILURE'
  channel = (Optional - defaults to '#builds') Slack channel to post: (eg) #builds
  color = (Optional - defaults to based on currentBuild.currentResult) Value that can either be one of 'good', 'warning', 'danger', or any hex color code
```

## Build Started
![build-started](media/01.gif)

## Build Passed
![build-pass](media/02.gif)

## Build Passed With Custom Message
![build-pass](media/06.gif)

## Build Failed (Failing Tests)
![build-fail-tests](media/05.gif)

## Build Failed (Not Enough Test Coverage)
![build-fail-tests](media/03.gif)

## Build Failed (Error in Pipeline)
![build-fail-tests](media/04.gif)