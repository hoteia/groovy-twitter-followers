
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.util.Date;
import groovy.json.*

/**
 *
 */
class ProtectedFriendManager {

	Twitter twitter
	def appDatasXml
		
	def userName
	
    ProtectedFriendManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
    }

	def isNotInitialized(){
		final Map protectedFriendsMap = DataManager.getProtectedFriendsMap(appDatasXml);
		
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
		final Map protectedFriendsMap = DataManager.getProtectedFriendsMap(appDatasXml);

		def protectedFollowingSize = protectedFriendsMap.keySet().size();
		println "is empty -> start init protected following"
		long cursor = -1;
		
		Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
		RateLimitStatus rateLimitFriends = RateUtil.checkRateLimitFriends(rateLimitStatusMap, twitter)
		RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)

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
					println "CHECK RATIO : " + rateLimitUserShow.getRemaining() + " : " + countCallTwitterShowUser 
					if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
						ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
						rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
						countCallTwitterShowUser = 0
					}


					def user = twitter.showUser(friendId);
					countCallTwitterShowUser++
					println "$friendIdString : " + user.getScreenName() + " | " + user.getName()
					def twitterUserInfo = user.getScreenName() + ";" + user.getName();
					def twitterUser = new TwitterUser( id: user.getId(), name: user.getName(), screenName: user.getScreenName(), createdAt: user.getCreatedAt(), favouritesCount: user.getFavouritesCount(), friendsCount: user.getFriendsCount(), followersCount: user.getFollowersCount())
					protectedFriendsMap.put(friendIdString, twitterUser);
				} catch(TwitterException ex) { 
					println "TwitterException : " + ex.getMessage()
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset());
				}
			} else {
				println "Already exists: " + friendId
			}
		}

		// rewrite all the map : delete/write
		FileWriter fstream = new FileWriter(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/protected_friends.properties');
		BufferedWriter bufferedWriter = new BufferedWriter(fstream); 
		println "friends to write in the protected list: " + protectedFriendsMap.entrySet().size()
		protectedFriendsMap.each { key, value ->
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
	}

}