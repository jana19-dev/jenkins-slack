#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction


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
      title: "$env.JOB_NAME-$env.BUILD_NUMBER",
      title_link: "$env.BUILD_URL",
      color: "warning",
      text: "Build Started :see_no_evil: :hear_no_evil: :speak_no_evil:",
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
      title: "$env.JOB_NAME-$env.BUILD_NUMBER",
      title_link: "$env.BUILD_URL",
      color: "good",
      text: "Success after ${currentBuild.durationString} :awesome_dance: :disco_dance: :penguin_dance:",
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
  return [
    [
      title: "$env.JOB_NAME-$env.BUILD_NUMBER",
      title_link: "$env.BUILD_URL",
      color: "danger",
      text: "Failed after ${currentBuild.durationString} :crying_bear: :sad_pepe: :try_not_to_cry:",
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
    ],
    [
      title: "Error Details",
      color: "danger",
      text: '```'+"$globalError"+'```',
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
