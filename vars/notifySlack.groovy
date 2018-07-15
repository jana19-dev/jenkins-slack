#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction
import hudson.model.Actionable
import hudson.tasks.junit.CaseResult

def jobName = ""
def author = ""
def commitMessage = ""


def getJobName = {
  jobName = "${env.JOB_NAME}"
  // Strip the branch name out of the job name (ex: "Job Name/branch1" -> "Job Name")
  jobName = jobName.getAt(0..(jobName.indexOf('/') - 1))
}

def getLastCommitMessage = {
  commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}

def getGitAuthor = {
  def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
  author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
}

@NonCPS
def getTestSummary = { ->
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
def getFailedTests = { ->
    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    def failedTestsString = "```"

    if (testResultAction != null) {
      def failedTests = testResultAction.getFailedTests()

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
def call(String buildStatus = 'STARTED', String channel = '#general', String testSummary = "", String failedTests = "") {
  def populateGlobalVariables = {
    getJobName()
    getLastCommitMessage()
    getGitAuthor()
  }

  // build status of null means SUCCESS
  buildStatus =  buildStatus ?: 'SUCCESS'

  if (buildStatus == 'STARTED') {
    def details = """<p>${buildStatus}: Job '${jobName} [${env.BUILD_NUMBER}]':</p>
      <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${jobName} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""
    slackSend (color: "FFFF00", message: details)
  } else if (buildStatus == 'SUCCESS') {
    def attachments = buildSuccessMessage()
    notifySlack("", channel, attachments)
  } else {
    def attachments = buildFailureMessage()
    notifySlack("", channel, attachments)
  }
}

def buildSuccessMessage() {
  def testSummary = getTestSummary()
  return [
    [
      title: "${jobName}, build #${env.BUILD_NUMBER} :awesome_dance: :banana_dance: :disco_dance: :hamster_dance: :penguin_dance: :panda_dance: :pepper_dance:",
      title_link: "${env.BUILD_URL}",
      color: "good",
      text: "SUCCESS\n${author}",
      fields: [
        [
          title: "Branch",
          value: "$BRANCH_NAME",
          short: true
        ],
        [
          title: "Test Results",
          value: "${testSummary}",
          short: true
        ],
        [
          title: "Last Commit",
          value: "${commitMessage}",
          short: false
        ]
      ]
    ]
  ]
}

def buildFailureMessage() {
  def testSummary = getTestSummary()
  def failedTestsString = getFailedTests()
  return [
    [
      title: "${jobName}, build #${env.BUILD_NUMBER} :crying: :crying_bear: :sad_pepe: :sad_poop: :try_not_to_cry:",
      title_link: "${env.BUILD_URL}",
      color: "danger",
      text: "FAILED\n${author}",
      "mrkdwn_in": ["fields"],
      fields: [
        [
          title: "Branch",
          value: "$BRANCH_NAME",
          short: true
        ],
        [
          title: "Test Results",
          value: "${testSummary}",
          short: true
        ],
        [
          title: "Last Commit",
          value: "${commitMessage}",
          short: false
        ]
      ]
    ],
    [
      title: "Failed Tests",
      color: "danger",
      text: "${failedTestsString}",
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
  sh "curl -X POST --data-urlencode \'payload=${payload}\' ${slackURL}"
}