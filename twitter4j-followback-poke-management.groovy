@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.2')
@Grab(group='javax.mail', module='mail', version='1.4.7')

import twitter4j.*
import twitter4j.auth.*
import twitter4j.conf.*

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import groovy.time.*

def today = new Date()
def context = System.getProperty("context")
def mode = System.getProperty("mode")
def rootScriptDir = System.getProperty("rootScriptDir")
def config = new ConfigSlurper().parse(new File(rootScriptDir + 'conf/' + context + '/config.properties').toURL());

ConfigurationBuilder twitterConf = new ConfigurationBuilder();

twitterConf.setDebugEnabled(true)
  .setOAuthConsumerKey(config.oAuthConsumerKey)
  .setOAuthConsumerSecret(config.oAuthConsumerSecret)
  .setOAuthAccessToken(config.oAuthAccessToken)
  .setOAuthAccessTokenSecret(config.oAuthAccessTokenSecret);

println "-------------------"
println "-------START-------"
println "date: $today"
println "rootScriptDir: " + rootScriptDir
println "context: " + context
println "-------------------"		

SendEmail.sendNotificationMail("Start " + mode + " Poke Tweets");
  
TwitterFactory twitterFactory = new TwitterFactory(twitterConf.build());
Twitter twitter;
try {
	twitter = twitterFactory.getInstance();
} catch(TwitterException ex) { 
	println "TwitterException : " + ex.getMessage()
	Tools.pause(ex.getRateLimitStatus().getSecondsUntilReset());
	twitter = twitterFactory.getInstance();
}

def appDatasFile = new File(rootScriptDir + 'datas/' + context + '/app.datas' )
def appDatasXml = new XmlSlurper().parse(appDatasFile)
def userNode = appDatasXml.conf.user[0]
if(0 == userNode.size()){
	println "Init conf file with node user!"
	appDatasXml.conf.appendNode { user() }
	// TODO  refactoring the xml reloading
	appDatasFile.withWriter { outWriter ->
		XmlUtil.serialize( new StreamingMarkupBuilder().bind{ mkp.yield appDatasXml }, outWriter )
	}
	appDatasFile = new File(rootScriptDir + 'datas/' + context + '/app.datas' )
	appDatasXml = new XmlSlurper().parse(appDatasFile)
}

def userId = appDatasXml.conf.user.@id.text()
boolean userIdDoesntExist = !userId?.trim()
if(userIdDoesntExist){
	userId = twitter.getId()
	println "App data doesn't contains user id : $userId"
	appDatasXml.conf.user[0].@id = "" + userId
}
def userName = appDatasXml.conf.user.@screenName.text()
boolean userNameDoesntExist = !userName?.trim()
if(userNameDoesntExist){
	userName = twitter.getScreenName()
	println "App data doesn't contains userName : $userName"
	appDatasXml.conf.user[0].@screenName = userName
}
println "User, ID : $userId , name : $userName"


if("add.poke" == mode){
	println "Mode Add poke Tweets"

	println "-------------------"
	println "STEP 1 - Add poke tweets"
	println "-------------------"

	PokeManager favoriteManager = new PokeManager(twitter, appDatasXml)
	favoriteManager.startAddPokeTweets(config.maxNewPokes)

} else if ("clean.poke" == mode){
	println "Mode Clean poke Tweets"
	
	println "-------------------"
	println "STEP 1 - Clean poke tweet"
	println "-------------------"
	
	PokeManager favoriteManager = new PokeManager(twitter, appDatasXml)
	favoriteManager.cleanPokeTweets()

}

appDatasFile.withWriter { outWriter ->
    XmlUtil.serialize( new StreamingMarkupBuilder().bind{ mkp.yield appDatasXml }, outWriter )
}

def end = new Date()
TimeDuration duration = TimeCategory.minus(end, today)
println "Processing end: " + end
println "Processing duration: " + duration
