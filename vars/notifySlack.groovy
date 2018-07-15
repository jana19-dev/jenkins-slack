#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction
import hudson.tasks.junit.CaseResult


/**
 * Send Slack notification to SLACK_CHANNEL based on errorOccured
 */
def call(errorOccured = null) {
  // errorOccured of null means ongoing build
  def attachments = []
  if (errorOccured == null) {
    attachments = buildStartingMessage()
  } else if (errorOccured == false) {
    attachments = buildSuccessMessage()
  } else {
    attachments = buildFailureMessage(errorOccured)
  }
  notifySlack("", SLACK_CHANNEL, attachments)
}


def buildStartingMessage() {
  return [
    [
      title: "$env.JOB_NAME-$env.BUILD_NUMBER :fingers_crossed:",
      title_link: "$env.BUILD_URL",
      color: "warning",
      text: "Started",
      fields: [
        [
          title: "Commit by $COMMIT_AUTHOR",
          value: COMMIT_MESSAGE,
          short: false
        ]
      ]
    ]
  ]
}

def buildSuccessMessage() {
  def testSummary = getTestSummary()
  return [
    [
      title: "$env.JOB_NAME-$env.BUILD_NUMBER  :awesome_dance: :banana_dance: :disco_dance: :hamster_dance: :penguin_dance: :panda_dance:",
      title_link: "$env.BUILD_URL",
      color: "good",
      text: "Success after ${currentBuild.durationString}",
      fields: [
        [
          title: "Commit by $COMMIT_AUTHOR",
          value: COMMIT_MESSAGE,
          short: true
        ],
        [
          title: "Test Results",
          value: testSummary,
          short: true
        ]
      ]
    ]
  ]
}

def buildFailureMessage(String globalError = "") {
  def testSummary = getTestSummary()
  def message = []
  message.add([
    title: "$env.JOB_NAME-$env.BUILD_NUMBER  :crying: :crying_bear: :sad_pepe: :sad_poop: :try_not_to_cry:",
    title_link: "$env.BUILD_URL",
    color: "danger",
    text: "Failed after ${currentBuild.durationString}",
    "mrkdwn_in": ["fields"],
    fields: [
      [
        title: "Commit by $COMMIT_AUTHOR",
        value: COMMIT_MESSAGE,
        short: true
      ],
      [
        title: "Test Results",
        value: "${testSummary}",
        short: true
      ]
    ]
  ])
  def failedTestsString = getAllFailedTests()
  if (failedTestsString!='``````') {
    message.add([
      title: "Failed Tests",
      color: "danger",
      text: failedTestsString,
      "mrkdwn_in": ["text"],
    ])
  }
  if (globalError) {
    message.add([
      title: "Error Details",
      color: "danger",
      text: globalError,
      "mrkdwn_in": ["text"],
    ])
  }
  return message
}

def notifySlack(text, channel, attachments) {
  def jenkinsIcon = 'https://wiki.jenkins-ci.org/download/attachments/2916393/logo.png'
  def payload = JsonOutput.toJson([
    text: text,
    channel: channel,
    username: "Jenkins",
    icon_url: jenkinsIcon,
    attachments: attachments
  ])
  withCredentials([string(credentialsId: '160a1dfe-afa8-47f1-8867-19b88ee52530', variable: 'slackURL')]) {
    sh "curl -X POST --data-urlencode \'payload=$payload\' $slackURL"
  }
}


@NonCPS
def getTestSummary() {
  def summary = "No tests found"
  def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
  if (testResultAction != null) {
    def total = testResultAction.getTotalCount()
    def failed = testResultAction.getFailCount()
    def skipped = testResultAction.getSkipCount()
    summary = "Passed: " + (total - failed - skipped)
    summary = summary + (", Failed: " + failed)
    summary = summary + (", Skipped: " + skipped)
  }
  return summary
}

@NonCPS
def getAllFailedTests() {
  def failedTestsString = "```"
  def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
  if (testResultAction != null) {
    failedTests = testResultAction.getFailedTests()
    for(CaseResult cr : failedTests) {
      failedTestsString = failedTestsString + "${cr.getFullDisplayName()}\n"
    }
  }
  failedTestsString = failedTestsString + "```"
  return failedTestsString
}