#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction


/**
 * Send a Slack notification based on given config values
 */
def call(Map config) {
  status = config.get('status', 'STARTED')
  message = config.get('message', '')
  channel = config.get('channel', '#builds')
  branchName = env.GIT_BRANCH.getAt((env.GIT_BRANCH.indexOf('/')+1..-1))
  commitMessage = config.get('commitMessage', '')
  commitAuthor = config.get('commitAuthor', '')
  sh "echo ${WORKSPACE}"
  def color, text
  def fields = []
  if (commitMessage != '') {
    fields.add([
      title: "Commit by $commitAuthor",
      value: commitMessage,
      short: true
    ])
  }
  if (branchName != '') {
    fields.add([
      title: "Branch Name",
      value: branchName,
      short: true
    ])
  }
  def testSummary = getTestSummary()
  if (status == 'STARTED') {
    color = "warning"
    text = "Build Started :see_no_evil: :hear_no_evil: :speak_no_evil:"
  } else if (status == 'SUCCESS') {
    color = "good"
    text = "Success after ${currentBuild.durationString} :awesome_dance: :disco_dance: :penguin_dance:"
    if (testSummary!="") fields.add([title: "Test Results", value: testSummary, short: true])
  } else { // status == "FAILURE"
    color = "danger"
    text = "Failed after ${currentBuild.durationString} :crying_bear: :sad_pepe: :try_not_to_cry:"
    if (testSummary!="") fields.add([title: "Test Results", value: testSummary, short: true])
  }
  def summary = [[
    title: "$env.JOB_NAME-$env.BUILD_NUMBER",
    title_link: "$env.BUILD_URL",
    color: color,
    "mrkdwn_in": ["fields"],
    fields: fields
  ]]
  if (message != '') {
    summary.add([
      title: "Details",
      color: "danger",
      text: '```'+"$message"+'```',
      "mrkdwn_in": ["text"],
    ])
  }
  sendMessage(text, channel, summary)
}

def sendMessage(text, channel, attachments) {
  def jenkinsIcon = 'http://www.perfecto.io/wp-content/uploads/2017/12/jenkins-and-perfecto.png'
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
  def summary = ""
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