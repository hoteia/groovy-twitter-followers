@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.2')
@Grab(group='javax.mail', module='mail', version='1.4.7')

import twitter4j.*
import twitter4j.auth.*
import twitter4j.conf.*

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import groovy.time.*

import groovy.json.*

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

def userFolder = new File(rootScriptDir + 'logs/' + userName + '/')
if(!userFolder.exists()){
	userFolder.mkdirs()  
}

println "---------------------------------------"
println "Migrate  init                          "
println "---------------------------------------"

Map protectedFriendsMap = DataManager.getProtectedFriendsMap(appDatasXml)
Map sourceProspectsMap = DataManager.getSourceProspects(appDatasXml);
Map blockedByUsersMap = DataManager.getBlockedByUsers(appDatasXml);
Map historicIdsMap = DataManager.getHistoricIds(appDatasXml);

Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
def countCallTwitterUserShow = 1


println "--------------------------------------------"
println "Migrate  protected friends prospects        "
println "--------------------------------------------"

Map newProtectedFriendsMap = new HashMap()

protectedFriendsMap.each { key, value ->
	println "## Old value key: '$key', value: '$value'"
	println "## Count: '$countCallTwitterUserShow', remaining: " + rateLimitUserShow.getRemaining()
	try{
		if(!value.startsWith("{")){
			if(countCallTwitterUserShow >= (rateLimitUserShow.getRemaining() - 1)){
				ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
				rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
				countCallTwitterUserShow = 0
			}
								
			long twitterId = new Long(key).longValue();
			User showUser = twitter.showUser(twitterId);
			countCallTwitterUserShow++
			def twitterUser = new TwitterUser( id: showUser.getId(), name: showUser.getName(), screenName: showUser.getScreenName(), createdAt: showUser.getCreatedAt(), favouritesCount: showUser.getFavouritesCount(), friendsCount: showUser.getFriendsCount(), followersCount: showUser.getFollowersCount(), usedAsProspect:sourceProspectsMap.containsKey(key), followDryOut:false)
			println new JsonBuilder(twitterUser).toString()
			newProtectedFriendsMap.put(key, twitterUser)
		}

	} catch(TwitterException ex) { 
		println "TwitterException : " + ex.getMessage()

		def errorCode = ex.getErrorCode()
		if(34 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, exist: false)
			println new JsonBuilder(twitterUser).toString()
			newProtectedFriendsMap.put(key, twitterUser)
		} else if(63 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, suspended: true)
			println new JsonBuilder(twitterUser).toString()
			newProtectedFriendsMap.put(key, twitterUser)
		} else {
			for (String endpoint : rateLimitStatusMap.keySet()) {
				RateLimitStatus status = rateLimitStatusMap.get(endpoint);
				if(status.getRemaining() < 3){
					System.out.println(" ** Endpoint: " + endpoint);
					System.out.println(" ** Limit: " + status.getLimit());
					System.out.println(" ** Remaining: " + status.getRemaining());
					System.out.println(" ** ResetTimeInSeconds: " + status.getResetTimeInSeconds());
					System.out.println(" ** SecondsUntilReset: " + status.getSecondsUntilReset());
				}
			}
			if(ex.getRateLimitStatus() != null){
				ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
			}
		}

	} catch(Exception ex) { 
		System.err.println("An Exception was caught!")
		ex.printStackTrace(); 
	}
}

// rewrite all the map : delete/write
FileWriter fstream = new FileWriter(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/protected_friends.properties');
BufferedWriter bufferedWriter = new BufferedWriter(fstream); 
println "friends to write in the new protected list: " + newProtectedFriendsMap.entrySet().size()
newProtectedFriendsMap.each { key, value ->
	try{
		bufferedWriter.append(key + "=" + new JsonBuilder(value).toString() + "\n");
	} catch(IOException ex) { 
		System.err.println("An IOException was caught!"); 
		ex.printStackTrace(); 
	}
}
try { 
	bufferedWriter.close(); 
} catch (IOException ex) { 
	System.err.println("An IOException was caught!"); 
	ex.printStackTrace(); 
} 
		
println "-----------------------------------"
println "Migrate friends prospects          "
println "-----------------------------------"

Map newSourceProspectsMap = new HashMap()

sourceProspectsMap.each { key, value ->
	println "## Old value key: '$key', value: '$value'"
	println "## Count: '$countCallTwitterUserShow', remaining: " + rateLimitUserShow.getRemaining()
	try{
		if(!value.startsWith("{")){
			if(countCallTwitterUserShow >= (rateLimitUserShow.getRemaining() - 1)){
				ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
				rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
				countCallTwitterUserShow = 0
			}
			
			long twitterId = new Long(key).longValue();
			User showUser = twitter.showUser(twitterId);
			countCallTwitterUserShow++
			def twitterUser = new TwitterUser( id: showUser.getId(), name: showUser.getName(), screenName: showUser.getScreenName(), createdAt: showUser.getCreatedAt(), favouritesCount: showUser.getFavouritesCount(), friendsCount: showUser.getFriendsCount(), followersCount: showUser.getFollowersCount(), usedAsProspect:sourceProspectsMap.containsKey(key), followDryOut:false)
			println new JsonBuilder(twitterUser).toString()
			newSourceProspectsMap.put(key, twitterUser)
		}
	} catch(TwitterException ex) { 
		println "TwitterException : " + ex.getMessage()
		
		def errorCode = ex.getErrorCode()
		if(34 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, exist: false)
			println new JsonBuilder(twitterUser).toString()
			newSourceProspectsMap.put(key, twitterUser)
		} else if(63 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, suspended: true)
			println new JsonBuilder(twitterUser).toString()
			newSourceProspectsMap.put(key, twitterUser)
		} else {
			for (String endpoint : rateLimitStatusMap.keySet()) {
				RateLimitStatus status = rateLimitStatusMap.get(endpoint);
				if(status.getRemaining() < 3){
					System.out.println(" ** Endpoint: " + endpoint);
					System.out.println(" ** Limit: " + status.getLimit());
					System.out.println(" ** Remaining: " + status.getRemaining());
					System.out.println(" ** ResetTimeInSeconds: " + status.getResetTimeInSeconds());
					System.out.println(" ** SecondsUntilReset: " + status.getSecondsUntilReset());
				}
			}
			if(ex.getRateLimitStatus() != null){
				ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
			}
		}
		
	} catch(Exception ex) { 
		System.err.println("An Exception was caught!")
		ex.printStackTrace(); 
	}
}

// rewrite all the map : delete/write
fstream = new FileWriter(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/friends_prospects.properties');
bufferedWriter = new BufferedWriter(fstream); 
println "friends to write in the new friend prospect list: " + newSourceProspectsMap.entrySet().size()
newSourceProspectsMap.each { key, value ->
	try{
		bufferedWriter.append(key + "=" + new JsonBuilder(value).toString() + "\n");
	} catch(IOException ex) { 
		System.err.println("An IOException was caught!"); 
		ex.printStackTrace(); 
	}
}
try { 
	bufferedWriter.close(); 
} catch (IOException ex) { 
	System.err.println("An IOException was caught!"); 
	ex.printStackTrace(); 
} 

println "----------------------------------"
println "Migrate blocked users             "
println "----------------------------------"

Map newBlockedByUsersMap = new HashMap()

blockedByUsersMap.each { key, value ->
	println "## Old value key: '$key', value: '$value'"
	println "## Count: '$countCallTwitterUserShow', remaining: " + rateLimitUserShow.getRemaining()
	try{
		if(!value.startsWith("{")){
			if(countCallTwitterUserShow >= (rateLimitUserShow.getRemaining() - 1)){
				ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
				rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
				countCallTwitterUserShow = 0
			}
			
			long twitterId = new Long(key).longValue();
			User showUser = twitter.showUser(twitterId);
			countCallTwitterUserShow++	
			def twitterUser = new TwitterUser( id: showUser.getId(), name: showUser.getName(), screenName: showUser.getScreenName(), createdAt: showUser.getCreatedAt(), favouritesCount: showUser.getFavouritesCount(), friendsCount: showUser.getFriendsCount(), followersCount: showUser.getFollowersCount(), usedAsProspect:sourceProspectsMap.containsKey(key), followDryOut:false)
			println new JsonBuilder(twitterUser).toString()
			newBlockedByUsersMap.put(key, twitterUser)
		}
		
	} catch(TwitterException ex) { 
		println "TwitterException : " + ex.getMessage()
		
		def errorCode = ex.getErrorCode()
		if(34 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, exist: false)
			println new JsonBuilder(twitterUser).toString()
			newBlockedByUsersMap.put(key, twitterUser)
		} else if(63 == errorCode){
			def twitterUser = new TwitterUser( id: new Long(key).longValue(), name: value, suspended: true)
			println new JsonBuilder(twitterUser).toString()
			newBlockedByUsersMap.put(key, twitterUser)
		} else {
			for (String endpoint : rateLimitStatusMap.keySet()) {
				RateLimitStatus status = rateLimitStatusMap.get(endpoint);
				if(status.getRemaining() < 3){
					System.out.println(" ** Endpoint: " + endpoint);
					System.out.println(" ** Limit: " + status.getLimit());
					System.out.println(" ** Remaining: " + status.getRemaining());
					System.out.println(" ** ResetTimeInSeconds: " + status.getResetTimeInSeconds());
					System.out.println(" ** SecondsUntilReset: " + status.getSecondsUntilReset());
				}
			}
			if(ex.getRateLimitStatus() != null){
				ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
			}
		}
		
	} catch(Exception ex) { 
		System.err.println("An Exception was caught!")
		ex.printStackTrace(); 
	}
}

// rewrite all the map : delete/write
fstream = new FileWriter(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/blocked_by_user.properties');
bufferedWriter = new BufferedWriter(fstream); 
println "friends to write in the new blocked list: " + newBlockedByUsersMap.entrySet().size()
newBlockedByUsersMap.each { key, value ->
	try{
		bufferedWriter.append(key + "=" + new JsonBuilder(value).toString() + "\n");
	} catch(IOException ex) { 
		System.err.println("An IOException was caught!"); 
		ex.printStackTrace(); 
	}
}
try { 
	bufferedWriter.close(); 
} catch (IOException ex) { 
	System.err.println("An IOException was caught!"); 
	ex.printStackTrace(); 
} 


appDatasFile.withWriter { outWriter ->
    XmlUtil.serialize( new StreamingMarkupBuilder().bind{ mkp.yield appDatasXml }, outWriter )
}
