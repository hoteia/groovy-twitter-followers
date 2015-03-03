
import java.util.Date;

import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

/**
 *
 */
class ProtectedFriendList {

	Twitter twitter
	def rootScriptDir = System.getProperty("rootScriptDir")
	def userName
	
    ProtectedFriendList(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.userName = appDatasXml.conf.user.@screenName.text()
		def userFolder = new File(rootScriptDir + 'datas/' + userName + '/')
		if(!userFolder.exists()){
			userFolder.mkdirs()  
		}
    }

	def isNotInitialized(){
		final Map protectedFriendsMap = getProtectedFriendsMap();
		if(protectedFriendsMap.size() > 0){
			IDs ids = twitter.getFriendsIDs(-1);
			long[] longIds = ids.getIDs();
			for (int i = 0; i < longIds.length; i++) {
				long friendId = longIds[i];
				String friendIdString = "" + friendId;
				if(!protectedFriendsMap.containsKey(friendIdString)){
					println "PROTECTED FRIENDS LIST IS NOT FULLY INITIALIZED -> FINISH"
					return true;
				}
			}
			println "PROTECTED FRIENDS LIST IS INITIALIZED"
			return false;
		} else {
			println "PROTECTED FRIENDS LIST IS NOT INITIALIZED"
			return true;
		}
	}
	
	def initialize(){
		final Map protectedFriendsMap = getProtectedFriendsMap();

		def protectedFollowingSize = protectedFriendsMap.keySet().size();
		println "is empty -> start init protected following"
		long cursor = -1;
		
// CHECK RATIO
Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
//System.out.println("RateLimit ResetTimeInSeconds: " + rateLimitStatusApplicationRateLimit.getResetTimeInSeconds());
System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status")
}

RateLimitStatus rateLimitStatusFriends = rateLimitStatusMap.get("/friends/ids");
System.out.println("Friends Limit: " + rateLimitStatusFriends.getLimit());
System.out.println("Friends Remaining: " + rateLimitStatusFriends.getRemaining());
//System.out.println("Friends ResetTimeInSeconds: " + rateLimitStatusFriends.getResetTimeInSeconds());
System.out.println("Friends SecondsUntilReset: " + rateLimitStatusFriends.getSecondsUntilReset());
if(rateLimitStatusFriends.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusFriends.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusFriends = rateLimitStatusMap.get("/friends/ids")
}

RateLimitStatus rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id");
System.out.println("UsersShow Limit: " + rateLimitStatusUsers.getLimit());
System.out.println("UsersShow Remaining: " + rateLimitStatusUsers.getRemaining());
//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusUsers.getResetTimeInSeconds());
System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusUsers.getSecondsUntilReset());
if(rateLimitStatusUsers.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
}

		IDs ids = twitter.getFriendsIDs(-1);
		long[] longIds = ids.getIDs();
		println "friends to evaluate: " + longIds.length
		def countCallTwitterShowUser = 0;
		for (int i = 0; i < longIds.length; i++) {
			long friendId = longIds[i];
			String friendIdString = "" + friendId;
			if(!protectedFriendsMap.containsKey(friendIdString)){
				try {
				
// CHECK RATIO
println "CHECK RATIO : " + rateLimitStatusUsers.getRemaining() + " : " + countCallTwitterShowUser 
if(countCallTwitterShowUser >= (rateLimitStatusUsers.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
	countCallTwitterShowUser = 0
}


					def user = twitter.showUser(friendId);
					countCallTwitterShowUser++
					println "$friendIdString : " + user.getScreenName() + " | " + user.getName()
					def twitterUserInfo = user.getScreenName() + ";" + user.getName();
					protectedFriendsMap.put(friendIdString, twitterUserInfo);
				} catch(TwitterException ex) { 
					println "TwitterException : " + ex.getMessage()
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset());
				}
			} else {
				println "Already exists: " + friendId
			}
		}

		// rewrite all the map : delete/write
		FileWriter fstream = new FileWriter(rootScriptDir + 'datas/' + userName + '/protected_friends.properties');
		BufferedWriter bufferedWriter = new BufferedWriter(fstream); 
		println "friends to write in the protected list: " + protectedFriendsMap.entrySet().size()
		protectedFriendsMap.each { key, value ->
			try{
				bufferedWriter.append(key + "=" + value + "\n");
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
	}

	def getProtectedFriendsMap(){
		final Map protectedFriendsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(rootScriptDir + 'datas/' + userName + '/protected_friends.properties');
			if (!protectedFollowingFile.exists()) {
				protectedFollowingFile.createNewFile()  
				return protectedFriendsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(protectedFollowingFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def twitterUserScreenName = split[1].split(";");
						protectedFriendsMap.put(twitterUserId, split[1]);					
					}
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			} finally { 
				try { 
					rd.close(); 
				} catch (IOException ex) { 
					System.err.println("An IOException was caught!"); 
					ex.printStackTrace(); 
				} 
			} 
		} catch (e) {
			throw e
		}
		return protectedFriendsMap;
	}

}