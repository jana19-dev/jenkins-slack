#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction
import hudson.tasks.junit.CaseResult


@NonCPS
def getTestSummary() {
  def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
  def summary = ""

  if (testResultAction != null) {
    def total = testResultAction.getTotalCount()
    def failed = testResultAction.getFailCount()
    def skipped = testResultAction.getSkipCount()

    summary = "Passed: " + (total - failed - skipped)
    summary = summary + (", Failed: " + failed)
    summary = summary + (", Skipped: " + skipped)
  }
  else {
    summary = "No tests found"
  }
  return summary
}

@NonCPS
def getAllFailedTests() {
    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    def failedTestsString = "```"

    if (testResultAction != null) {
      failedTests = testResultAction.getFailedTests()

      if (failedTests.size() > 9) {
        failedTests = failedTests.subList(0, 8)
      }

      for(CaseResult cr : failedTests) {
        failedTestsString = failedTestsString + "${cr.getFullDisplayName()}\n"
      }
      failedTestsString = failedTestsString + "```"
    }
    return failedTestsString
}

/**
 * Send Slack notification to the channel given based on errorOccurred string
 */
def call(errorOccurred = null) {
  // build status of null means ongoing build
  def attachments = []
  if (errorOccurred == null) {
    attachments = buildStartingMessage()
  } else if (errorOccurred == false) {
    attachments = buildSuccessMessage()
  } else {
    attachments = buildFailureMessage(errorOccurred)
  }

  notifySlack("", CHANNEL, attachments)
}

def buildStartingMessage() {
  return [
    [
      title: "$env.BUILD_TAG :fingers_crossed:",
      title_link: "$env.BUILD_URL",
      color: "warning",
      text: "BUILD STARTED by $AUTHOR",
      fields: [
        [
          title: "Last Commit",
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
      title: "$env.BUILD_TAG :awesome_dance: :banana_dance: :disco_dance: :hamster_dance: :penguin_dance: :panda_dance: :pepper_dance:",
      title_link: "$env.BUILD_URL",
      color: "good",
      text: "SUCCESS by $AUTHOR",
      fields: [
        [
          title: "Last Commit",
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

def buildFailureMessage(, String globalError = "") {
  def testSummary = getTestSummary()
  def failedTestsString = getAllFailedTests()
  return [
    [
      title: "$env.BUILD_TAG :crying: :crying_bear: :sad_pepe: :sad_poop: :try_not_to_cry:",
      title_link: "$env.BUILD_URL",
      color: "danger",
      text: "FAILED by $AUTHOR",
      "mrkdwn_in": ["fields"],
      fields: [
        [
          title: "Last Commit",
          value: COMMIT_MESSAGE,
          short: true
        ],
        [
          title: "Test Results",
          value: "${testSummary}",
          short: true
        ]
      ]
    ],
    [
      title: "Failed Tests",
      color: "danger",
      text: failedTestsString,
      "mrkdwn_in": ["text"],
    ],
    [
      title: "General Error",
      color: "danger",
      text: globalError,
      "mrkdwn_in": ["text"],
    ]
  ]
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