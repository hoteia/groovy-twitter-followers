@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.2')
@Grab(group='javax.mail', module='mail', version='1.4.7')

import twitter4j.*
import twitter4j.auth.*
import twitter4j.conf.*

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

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
println "mode: " + mode
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
def userName = appDatasXml.conf.user.@screenName.text()
boolean userNameDoesntExist = !userName?.trim()
if(userNameDoesntExist){
	userName = twitter.getScreenName()
	println "App data doesn't contains userName : $userName"
	appDatasXml.conf.user[0].@screenName = userName
}
println "User, ID : $userId , name : $userName"

boolean modeExist = mode?.trim()
if(modeExist){

	if("add.followers" == mode){
		println "Mode Add Followers"

		println "-------------------"
		println "STEP 1 - CHECK AND EVALUATE PROTECTED FRIENDS LIST"
		println "-------------------"

		ProtectedFriendList protectedFriendList = new ProtectedFriendList(twitter, appDatasXml)
		if(protectedFriendList.isNotInitialized()){
			println "- START PROTECTED FRIENDS LIST INITIALIZE"
			protectedFriendList.initialize()
		}
		
		println "-------------------"
		println "STEP 2 - CHECK AND EVALUATE PROSPECT ACCOUNT SOURCE LIST"
		println "-------------------"

		SourceProspectList sourceProspectList = new SourceProspectList(twitter, appDatasXml)
		if(sourceProspectList.isNotUpToDate()){
			println "- START PROSPECT ACCOUNT SOURCE LIST INITIALIZE"
			sourceProspectList.initialize()
		}
		
		println "-------------------"
		println "STEP 3 - BUILD A FOLLOWERS LIST FROM PROSPECT SOURCE LIST"
		println "-------------------"

		Map sourceProspectsMap = MapUtil.getSourceProspects(appDatasXml);
		FollowerManager followerManager = new FollowerManager(twitter, appDatasXml);
		Map targetedfollowersMap = followerManager.initFollowersListToCheck(sourceProspectsMap, config.maxNewFollowers);
		followerManager.addFollowers(targetedfollowersMap);

		println "End Switch Mode App 'add.followers' to 'clean.followers' in xml file for the next run"
		appDatasXml.conf[0].mode[0] = "clean.followers"

	} else if ("clean.followers" == mode){
		println "Mode Clean Followers"
		
		println "-------------------"
		println "STEP 1 - CLEAN FOLLOWERS WHICH ARE NOT PROTECTED"
		println "-------------------"
		
		ProtectedFriendList protectedFriendList = new ProtectedFriendList(twitter, appDatasXml)
		Map protectedFriendsMap = MapUtil.getProtectedFriendsMap(appDatasXml)
		FollowerManager followerManager = new FollowerManager(twitter, appDatasXml);
		followerManager.deleteFollowers(protectedFriendsMap)

		println "End Switch Mode App 'clean.followers' to 'add.followers' in xml file for the next run"
		appDatasXml.conf[0].mode[0] = "add.followers"
	}

} else {
	println "Mode Add Followers"
	
	println "-------------------"
	println "STEP 0 - CHECK AND EVALUATE PROTECTED FRIENDS LIST"
	println "-------------------"

	// NO CHECK -> CREATE A NEW SCRIPT TO INIT
	ProtectedFriendList protectedFriendList = new ProtectedFriendList(twitter, appDatasXml)
	
	println "-------------------"
	println "STEP 1 - CLEAN FOLLOWERS WHICH ARE NOT PROTECTED"
	println "-------------------"
	
	Map protectedFriendsMap = MapUtil.getProtectedFriendsMap(appDatasXml)
	FollowerManager followerManager = new FollowerManager(twitter, appDatasXml);
	followerManager.deleteFollowers(protectedFriendsMap)
	
	println "-------------------"
	println "STEP 2 - CHECK AND EVALUATE PROSPECT ACCOUNT SOURCE LIST"
	println "-------------------"

	SourceProspectList sourceProspectList = new SourceProspectList(twitter, appDatasXml)
	if(sourceProspectList.isNotUpToDate()){
		println "- START PROSPECT ACCOUNT SOURCE LIST INITIALIZE"
		sourceProspectList.initialize()
	}
	
	println "-------------------"
	println "STEP 3 - BUILD A FOLLOWERS LIST FROM PROSPECT SOURCE LIST"
	println "-------------------"

	Map sourceProspectsMap = MapUtil.getSourceProspects(appDatasXml);
	Map targetedfollowersMap = followerManager.initFollowersListToCheck(sourceProspectsMap, config.maxNewFollowers);
	followerManager.addFollowers(targetedfollowersMap);

	println "End Switch Mode App 'add.followers' to 'clean.followers' in xml file for the next run"
	appDatasXml.conf[0].mode[0] = "clean.followers"
}	
	
appDatasFile.withWriter { outWriter ->
    XmlUtil.serialize( new StreamingMarkupBuilder().bind{ mkp.yield appDatasXml }, outWriter )
}
