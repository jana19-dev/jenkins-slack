#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction
import hudson.model.Actionable
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
def call(String buildStatus = 'STARTED', String channel = '#general', String commitMessage = "", String author = "") {
  def jobName = "$env.JOB_NAME"
  // Strip the branch name out of the job name (ex: "Job Name/branch1" -> "Job Name")
  jobName = jobName.getAt(0..(jobName.indexOf('/') - 1))

  //build status of null means SUCCESS
  // buildStatus =  buildStatus ?: 'SUCCESS'

  // def attachments = []
  // if (buildStatus == 'STARTED') {
  //   attachments = buildStartingMessage(jobName, commitMessage, author)
  // } else if (buildStatus == 'SUCCESS') {
  //   attachments = buildSuccessMessage(jobName, commitMessage, author)
  // } else {
  //   attachments = buildFailureMessage(jobName, commitMessage, author)
  // }

  // notifySlack("", channel, attachments)
}

def buildStartingMessage(String jobName = "", String commitMessage = "", String author = "") {
  return [
    [
      title: "$jobName, build #$env.BUILD_NUMBER :fingers_crossed:",
      title_link: "$env.BUILD_URL",
      color: "warning",
      text: "STARTING\n$author",
      fields: [
        [
          title: "Branch",
          value: "$env.BRANCH_NAME",
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

def buildSuccessMessage(String jobName = "", String commitMessage = "", String author = "") {
  def testSummary = getTestSummary()
  return [
    [
      title: "$jobName, build #$env.BUILD_NUMBER :awesome_dance: :banana_dance: :disco_dance: :hamster_dance: :penguin_dance: :panda_dance: :pepper_dance:",
      title_link: "$env.BUILD_URL",
      color: "good",
      text: "SUCCESS\n$author",
      fields: [
        [
          title: "Branch",
          value: "$env.BRANCH_NAME",
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

def buildFailureMessage(String jobName = "", String commitMessage = "", String author = "") {
  def testSummary = getTestSummary()
  def failedTestsString = getAllFailedTests()
  return [
    [
      title: "$jobName, build #$env.BUILD_NUMBER :crying: :crying_bear: :sad_pepe: :sad_poop: :try_not_to_cry:",
      title_link: "$env.BUILD_URL",
      color: "danger",
      text: "FAILED\n$author",
      "mrkdwn_in": ["fields"],
      fields: [
        [
          title: "Branch",
          value: "$env.BRANCH_NAME",
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
  def slackURL = credentials('160a1dfe-afa8-47f1-8867-19b88ee52530')
  def jenkinsIcon = 'https://wiki.jenkins-ci.org/download/attachments/2916393/logo.png'
  def payload = JsonOutput.toJson([text: text,
    channel: channel,
    username: "Jenkins",
    icon_url: jenkinsIcon,
    attachments: attachments
  ])
  sh "curl -X POST --data-urlencode \'payload=$payload\' $slackURL"
}