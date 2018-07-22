#!/usr/bin/env groovy

import groovy.json.JsonOutput
import hudson.tasks.test.AbstractTestResultAction


/**
 * Send a Slack notification based on given config values
 */
def call(Map config) {
  message = config.get('message', '')
  channel = config.get('channel', '#opstastic')
  color = config.get('color', 'warning')
  status = config.get('status', currentBuild.currentResult)
  try {
    branchName = env.GIT_BRANCH.getAt((env.GIT_BRANCH.indexOf('/')+1..-1))
    commitMessage = sh(returnStdout: true, script: "git log -1 --pretty=%B").trim() // Auto generated
    commitAuthor = sh(returnStdout: true, script: "git --no-pager show -s --format=%an").trim() // Auto generated
  } catch (e) {
    status = 'STARTED'
    branchName = ''
    commitMessage = ''
    commitAuthor = ''
  }
  def text
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
    text = "Build Started :see_no_evil: :hear_no_evil: :speak_no_evil:"
  } else if (status == 'SUCCESS') {
    color = "good"
    text = "Success after ${currentBuild.durationString} :awesome_dance: :disco_dance: :penguin_dance:"
    if (testSummary!="") fields.add([title: "Test Results", value: testSummary, short: true])
  } else if (status == 'UNSTABLE') {
    color = "#F28500"
    text = "Unstable after ${currentBuild.durationString} :thinking_face: :confused: :hand:"
    if (testSummary!="") fields.add([title: "Test Results", value: testSummary, short: true])
  } else { // status == "FAILURE"
    color = "danger"
    text = "Failed after ${currentBuild.durationString} :crying_bear: :crying: :sad_pepe:"
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
    color = status == 'SUCCESS' ? 'good' : '#F28500'
    summary.add([
      title: "Details",
      color: color,
      text: '```'+"$message"+'```',
      "mrkdwn_in": ["text"],
    ])
  }
  try {
    sendMessage(text, channel, summary)
  } catch (e) {
    slackSend color: color, message: message, channel: channel
  }
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
  withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'slackURL')]) {
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