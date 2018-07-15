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
        failedTestsString = failedTestsString + "${cr.getFullDisplayName()}:\n${cr.getErrorDetails()}\n\n"
      }
      failedTestsString = failedTestsString + "```"
    }
    return failedTestsString
}

/**
 * Send Slack notification to the channel given based on buildStatus string
 */
def call(String buildStatus, String channel = '#general', String branch = "", String commitMessage = "", String author = "") {

  // build status of null means ongoing build
  buildStatus = "$buildStatus" ?: 'STARTED'
  echo currentBuild.result
  def attachments = []
  if (buildStatus == 'STARTED') {
    attachments = buildStartingMessage(branch, commitMessage, author)
  } else if (buildStatus == 'SUCCESS') {
    attachments = buildSuccessMessage(branch, commitMessage, author)
  } else {
    attachments = buildFailureMessage(branch, commitMessage, author)
  }

  notifySlack("", channel, attachments)
}

def buildStartingMessage(String branch = "", String commitMessage = "", String author = "") {
  return [
    [
      title: "$env.JOB_BASE_NAME, build #$env.BUILD_NUMBER :fingers_crossed:",
      title_link: "$env.BUILD_URL",
      color: "warning",
      text: "STARTING\n$author",
      fields: [
        [
          title: "Branch",
          value: branch,
          short: true
        ],
        [
          title: "Last Commit",
          value: commitMessage,
          short: true
        ]
      ]
    ]
  ]
}

def buildSuccessMessage(String branch = "", String commitMessage = "", String author = "") {
  def testSummary = getTestSummary()
  return [
    [
      title: "$env.JOB_BASE_NAME, build #$env.BUILD_NUMBER :awesome_dance: :banana_dance: :disco_dance: :hamster_dance: :penguin_dance: :panda_dance: :pepper_dance:",
      title_link: "$env.BUILD_URL",
      color: "good",
      text: "SUCCESS\n$author",
      fields: [
        [
          title: "Branch",
          value: branch,
          short: true
        ],
        [
          title: "Test Results",
          value: testSummary,
          short: true
        ],
        [
          title: "Last Commit",
          value: commitMessage,
          short: false
        ]
      ]
    ]
  ]
}

def buildFailureMessage(String branch = "", String commitMessage = "", String author = "") {
  def testSummary = getTestSummary()
  def failedTestsString = getAllFailedTests()
  return [
    [
      title: "$env.JOB_BASE_NAME, build #$env.BUILD_NUMBER :crying: :crying_bear: :sad_pepe: :sad_poop: :try_not_to_cry:",
      title_link: "$env.BUILD_URL",
      color: "danger",
      text: "FAILED\n$author",
      "mrkdwn_in": ["fields"],
      fields: [
        [
          title: "Branch",
          value: branch,
          short: true
        ],
        [
          title: "Test Results",
          value: "${testSummary}",
          short: true
        ],
        [
          title: "Last Commit",
          value: commitMessage,
          short: false
        ]
      ]
    ],
    [
      title: "Failed Tests",
      color: "danger",
      text: failedTestsString,
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