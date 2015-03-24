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

SendEmail.sendNotificationMail("Start");
  
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
def userScreenName = appDatasXml.conf.user.@name.text()
boolean userScreenNameDoesntExist = !userScreenName?.trim()
if(userScreenNameDoesntExist){
	userScreenName = twitter.getScreenName()
	println "App data doesn't contains userScreenName : $userScreenName"
	appDatasXml.conf.user[0].@name = userScreenName
}
def userName = appDatasXml.conf.user.@name.text()
boolean userNameDoesntExist = !userName?.trim()
if(userNameDoesntExist){
	userName = twitter.getScreenName()
	println "App data doesn't contains userName : $userName"
	appDatasXml.conf.user[0].@name = userName
}
println "User, ID : $userId, name : $userName, screenName : $userScreenName"

def summary = "\n\r"

User user = twitter.showUser(twitter.getId());
		
println "-------------------"
println "STEP 1 - FRIENDS   "
println "-------------------"

def friendsCount = user.getFriendsCount()
println "Friends: " + friendsCount

summary += "Friends: " + friendsCount
summary += "\n\r"

println "-------------------"
println "STEP 1 - FOLLOWERS "
println "-------------------"

def followersCount = user.getFollowersCount()
println "Followers: " + followersCount

summary += "\n\r"
summary += "Followers: " + followersCount
summary += "\n\r"

println "-------------------"
println "STEP 2 - FAVORITES "
println "-------------------"

def favouritesCount = user.getFavouritesCount()
println "Favorites: " + favouritesCount

summary += "\n\r"
summary += "Favorites: " + favouritesCount
summary += "\n\r"

println "-------------------"
println "STEP 2 - PFRIENDS  "
println "-------------------"

Map protectedFriendsMap = DataManager.getProtectedFriendsMap(appDatasXml)
def protectedFriendsCount = protectedFriendsMap.size()
println "Protected Friends: " + protectedFriendsCount

summary += "\n\r"
summary += "Protected Friends: " + protectedFriendsCount
summary += "\n\r"

println "--------------------"
println "STEP 2 - SPROSPECT  "
println "--------------------"

Map sourceProspectsMap = DataManager.getSourceProspects(appDatasXml);
def totalSsourceProspectsCount = sourceProspectsMap.size()
def countSourceProspectNotDryOut = 0
sourceProspectsMap.each { key, twitterUserProspectJson ->
	if(!twitterUserProspectJson.followDryOut)
		countSourceProspectNotDryOut++
}
		
println "Friends Prospects available: $countSourceProspectNotDryOut on $totalSsourceProspectsCount"

summary += "\n\r"
summary += "Friends Prospects available: $countSourceProspectNotDryOut on $totalSsourceProspectsCount"
summary += "\n\r"

SendEmail.sendSummaryMail(summary, "" + followersCount);

	
appDatasFile.withWriter { outWriter ->
    XmlUtil.serialize( new StreamingMarkupBuilder().bind{ mkp.yield appDatasXml }, outWriter )
}

def end = new Date()
TimeDuration duration = TimeCategory.minus(end, today)
println "Processing end: " + end
println "Processing duration: " + duration
