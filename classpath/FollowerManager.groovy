
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
 
/**
 *
 */
class FollowerManager {

	Twitter twitter
	long[] myFollowers
	def appDatasXml
	
	def userName
	def userId
	
	def today = Calendar.instance
							
    FollowerManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
		this.userId = appDatasXml.conf.user.@id.text()
    }

	def initFollowersListToCheck(Map sourceProspectsMap, int maxNewFollowers){

		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
	
		final Map historicMap = MapUtil.getHistoricIds(appDatasXml);
		final Map ignorefollowersMap = MapUtil.getIgnoreFollowers(appDatasXml);
		final Map blockedByUsersMap = MapUtil.getBlockedByUsers(appDatasXml)
		final Map targetedfollowersMap = new HashMap();
		//def secureMaxWhileCountIt = 5
		def splitByProspectResult = (maxNewFollowers / sourceProspectsMap.size())
		def splitByProspect = splitByProspectResult.setScale(0, BigDecimal.ROUND_UP)
		println "maxNewFollowers: " + maxNewFollowers + ", sourceProspectsMap size:" + sourceProspectsMap.size() + ", splitByProspect:" + splitByProspectResult + "/" + splitByProspect
		if(sourceProspectsMap.size() > 0){
			println "INIT FOLLOWERS LIST TO CHECK IN HISTORY FILE"
			
			println "----------------------------------------------------------------"
			
			sourceProspectsMap.each { key, value ->
				println "------------------------------------------------"
				if(targetedfollowersMap.size() >= maxNewFollowers){
					return
				}
				try{
					long prospectTwitterId = new Long(key).longValue();						

					RateLimitStatus rateLimitStatusFollowers = RateUtil.checkRateLimitStatusFollowers(twitter)
					RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(twitter)

					boolean continueItIdsPagination = true;
					long cursor = -1;
					
					def countCallTwitterShowUser = 0;
					def countUserToAddByProspect = 0;
					
					while(continueItIdsPagination){
						IDs followerIds = twitter.getFollowersIDs(prospectTwitterId, cursor);
						println "******************************************************"
						println "### Target: $key/$value"
						println "### Page/cursor: $cursor"
						println "### Followers: " + followerIds.getIDs().length
						println "******************************************************"
						long[] longIds = followerIds.getIDs();

						for (int i = 0; i < longIds.length; i++) {
							if(targetedfollowersMap.size() >= maxNewFollowers){
								println "targetedfollowersMap.size() >= maxNewFollowers"
								continueItIdsPagination = false;
								break;								
							}
							if(countUserToAddByProspect >= splitByProspect){
								println "countUserToAddByProspect >= splitByProspect"
								continueItIdsPagination = false;
								break;
							}
							
							long followerId = longIds[i];
							String followerIdString = "" + followerId;
							println "### User: $followerId/"
							if(!historicMap.containsKey(followerIdString) && !ignorefollowersMap.containsKey(followerIdString) && !followerIdString.equals(userId)){
								try {
								
// CHECK RATIO
println "-------------------------------------------------------"
println "CHECK RATIO : Remaining = " + rateLimitUserShow.getRemaining() + " : ShowUser = " + countCallTwitterShowUser + " : targetedfollowersMap = " + targetedfollowersMap.size()
if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitUserShow = rateLimitStatusMap.get("/users/show/:id")
	countCallTwitterShowUser = 0
}
println "-------------------------------------------------------"

									User user = twitter.showUser(followerId);
									println "### User info: " + user.getScreenName() + "/" + user.getName() 
									countCallTwitterShowUser++
									Status status = user.getStatus();
									int userFollowersCount = user.getFollowersCount()
									def followRequestSent = user.isFollowRequestSent()
									boolean isNotFollowingMe = ScriptGroovyUtil.isNotFollowingMe(this.myFollowers, followerId)
									
									println "### Followers: " + userFollowersCount + " / follow request sent: " + followRequestSent + " / test isNotFollowingMe: "+ isNotFollowingMe + " / status not null: " + (status != null)
									println "### Test isNotFollowingMe: " + isNotFollowingMe + " / Status is not null: " + (status != null)
									if(!user.isFollowRequestSent() && isNotFollowingMe && userFollowersCount < 2000 && status != null){
										Relationship relationship = twitter.showFriendship(twitter.getId(), user.getId())
										println "### isSourceFollowedByTarget: " + relationship.isSourceFollowedByTarget()
										if(!relationship.isSourceFollowedByTarget()){
											def validDays = 2
											Date lastStatusDate = user.getStatus().getCreatedAt();
											def validDateTime = today.time - validDays
											def ignoreDateTime = today.time - 30
											println "Last statut is recent < $validDays days: " + (lastStatusDate.time >= validDateTime) + " -> " +lastStatusDate
											println "Last statut is recent < $validDays days: " + (lastStatusDate.time >= validDateTime) + " -> " +lastStatusDate
											println "Last statut is not recent >30, ignore: " + (lastStatusDate.time <= validDateTime) + " -> " +lastStatusDate
											
											if(lastStatusDate.time >= validDateTime){
												targetedfollowersMap.put(user.getId(), user)
												println "- Keep - Total users to add: " + targetedfollowersMap.size()
												countUserToAddByProspect++
											} else if (lastStatusDate.time <= ignoreDateTime){
												def twitterUserInfo = user.getScreenName() + ";" + user.getName();
												ignorefollowersMap.put(user.getId(), twitterUserInfo)
												println "- Ignore - ignore followers Map size:  " + ignorefollowersMap.size()
											}
										}
									} else {
										println "- Skip -"
									}
									
								} catch(TwitterException ex) { 
									println "TwitterException : " + ex.getMessage()
									if(ex.getRateLimitStatus() != null){
										ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
									}
								}
							} else {
								println "- No showUser -"
								if(historicMap.containsKey(followerIdString)){
									println "historicMap containsKey $followerIdString: " + historicMap.containsKey(followerIdString)
								}
								if(ignorefollowersMap.containsKey(followerIdString)){
									println "ignorefollowersMap containsKey $followerIdString: " + ignorefollowersMap.containsKey(followerIdString)
								}
								if(followerIdString.equals(userId)){
									println "followerIdString.equals(userId): " + userId
								}
							}
							println "******************************************************"
						}
						
						
						cursor = followerIds.getNextCursor();
						if(!followerIds.hasNext()){
							continueItIdsPagination = false;
						}
					}


					
				} catch(TwitterException ex) { 
					println "TwitterException : " + ex.getMessage()
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
				} catch(IOException ex) { 
					System.err.println("An IOException was caught!")
					ex.printStackTrace(); 
				}
			}	
			println "targetedfollowersMap : " + targetedfollowersMap.size()

		} else {
			println "SOURCE PROSPECT LIST IS NOT INITIALIZED"
		}
		
		
		// SAVE IGNORE FOLLOWER
		// rewrite all the map : delete/write
		println "added users to write in the ignore list: " + ignorefollowersMap.entrySet().size()
		File file = new File(rootScriptDir + 'datas/' + userName + '/ignore_followers.properties');
		ignorefollowersMap.each { key, value ->
			try{
				 file << (key + "=" + value + "\n")
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			}
		}
		
		return targetedfollowersMap;
	}
	
	def addFollowers(Map targetedfollowersMap){
		final Map addedFollowersMap = new HashMap()
		final Map blockUsersMap = new HashMap()
		targetedfollowersMap.each { key, value ->
			def twitterUserInfo = value.getScreenName() + ";" + value.getName();
			try{
				long prospectTwitterId = new Long(key).longValue();
				addedFollowersMap.put(key, twitterUserInfo)
				twitter.createFriendship(prospectTwitterId)
				println "Add follower $key/$twitterUserInfo"
				
			} catch(TwitterException ex) { 
				addedFollowersMap.put(key, twitterUserInfo)

				Writer wr = new StringWriter();
				PrintWriter pWriter = new PrintWriter(wr);
				ex.printStackTrace(pWriter);
				def errorMessage = "Error to add follower $key/$twitterUserInfo" + wr.toString()
				SendEmail.sendErrorMail(errorMessage);
				println "TwitterException : " + ex.getMessage()
				
				def errorCode = ex.getErrorCode()
				if('162' == errorCode){
					blockUsersMap.put(key, twitterUserInfo)
				}
				
				//ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
				return

			} catch(IOException ex) { 
				Writer wr = new StringWriter();
				PrintWriter pWriter = new PrintWriter(wr);
				ex.printStackTrace(pWriter);
				SendEmail.sendErrorMail(wr.toString());
				System.err.println("An IOException was caught!")
				ex.printStackTrace();
				
			} catch(Exception ex) { 
				Writer wr = new StringWriter();
				PrintWriter pWriter = new PrintWriter(wr);
				ex.printStackTrace(pWriter);
				SendEmail.sendErrorMail(wr.toString());
				ex.getMessage()
			}
		}
		println "blocked from: " + blockUsersMap.size() + " user(s)"
		// TODO : histo the map in a file
		
		
		// SAVE HISTORY
		// rewrite all the map : delete/write
		println "added friend to write in the history list: " + addedFollowersMap.size()
		File file = new File(rootScriptDir + 'datas/' + userName + '/history_add_followers.properties');
		addedFollowersMap.each { key, value ->
			try{
				 file << (key + "=" + value + "\n")
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			}
		}
		SendEmail.sendSuccessMail("new followers", addedFollowersMap.size());
	}
	
	def deleteFollowers(Map protectedFriendsMap){
		long cursor = -1;
		IDs ids = twitter.getFriendsIDs(-1);
		long[] longIds = ids.getIDs();
		println "friends to evaluate: " + longIds.length
		println "friends protected: " + protectedFriendsMap.size()
		final Map deletedFollowersMap = new HashMap()
		for (int i = 0; i < longIds.length; i++) {
			long friendId = longIds[i];
			String friendIdString = "" + friendId;
			if(!protectedFriendsMap.containsKey(friendIdString)){
				try {
					twitter.destroyFriendship(friendId)
					deletedFollowersMap.put(friendIdString, friendIdString)
					
				} catch(TwitterException ex) { 
					Writer wr = new StringWriter();
					PrintWriter pWriter = new PrintWriter(wr);
					ex.printStackTrace(pWriter);
					SendEmail.sendErrorMail(wr.toString());
					println "TwitterException : " + ex.getMessage()
					//ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
					return
				
				} catch(Exception ex) { 
					Writer wr = new StringWriter();
					PrintWriter pWriter = new PrintWriter(wr);
					ex.printStackTrace(pWriter);
					SendEmail.sendErrorMail(wr.toString());
					ex.getMessage()
				}
			}
		}
		println "Deleted followers: " + deletedFollowersMap.size()
		SendEmail.sendSuccessMail("delete followers", deletedFollowersMap.size());
	}
	
	def searchFollowersToAdd(String word, int maxNewFollowers){

		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
		final Map historicMap = MapUtil.getHistoricIds(appDatasXml);
		final Map targetedfollowersMap = new HashMap();
		int page = 1;
		def ignoreDateTime = today.time - 30
		while(page < 20){
			ResponseList<User> users = twitter.searchUsers(word, page);
			for (User user : users) {
				if(targetedfollowersMap.size() >= maxNewFollowers){
					break;
				}
				String userIdString = "" + user.getId()
				if(!historicMap.containsKey(userIdString)){
					if (null != user.getStatus()) {
						def validDays = 2
						Date lastStatusDate = user.getStatus().getCreatedAt();
						def validDateTime = today.time - validDays
						if(lastStatusDate.time >= validDateTime.time){
							targetedfollowersMap.put(user.getId(), user)
							println "Keep this User, total: " + targetedfollowersMap.size()
						} 
					}
				}
			}
			page++;
		}
		return targetedfollowersMap;
	}


	
	def cleanFollowers(String word){
	
// CHECK RATIO
Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status")
System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
}

RateLimitStatus rateLimitUserShow = rateLimitStatusMap.get("/users/show/:id")
System.out.println("UsersShow Limit: " + rateLimitUserShow.getLimit());
System.out.println("UsersShow Remaining: " + rateLimitUserShow.getRemaining());
//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitUserShow.getResetTimeInSeconds());
System.out.println("UsersShow SecondsUntilReset: " + rateLimitUserShow.getSecondsUntilReset());
if(rateLimitUserShow.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitUserShow = rateLimitStatusMap.get("/users/show/:id")
}


		long cursor = -1;
		IDs ids = twitter.getFriendsIDs(-1);
		long[] longIds = ids.getIDs();
		println "friends to evaluate: " + longIds.length
		final Map deletedFollowersMap = new HashMap()
		def countCallTwitterShowUser = 0
		for (int i = 0; i < longIds.length; i++) {
			long friendId = longIds[i];
			String friendIdString = "" + friendId;
			

				try {
// CHECK RATIO
println "CHECK RATIO : Remaining = " + rateLimitUserShow.getRemaining()  + ", countCallTwitterShowUser:" + countCallTwitterShowUser + ", deletedFollowersMap: " + deletedFollowersMap.size()
if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitUserShow = rateLimitStatusMap.get("/users/show/:id")
	countCallTwitterShowUser = 0
}

					User user = twitter.showUser(friendId);
					countCallTwitterShowUser++
					def userScreenName = user.getScreenName()
					def userName = user.getName()
					println "check screenName: " + userScreenName + ", userName: " + userName + ", with the word " + word
					if(userScreenName.toLowerCase().contains(word.toLowerCase()) || userName.toLowerCase().contains(word.toLowerCase())){
						println "delete user: " + userScreenName
//						twitter.destroyFriendship(friendId)
						deletedFollowersMap.put(friendIdString, friendIdString)
					}
					
				} catch(TwitterException ex) { 
					Writer wr = new StringWriter();
					PrintWriter pWriter = new PrintWriter(wr);
					ex.printStackTrace(pWriter);
					SendEmail.sendErrorMail(wr.toString());
					println "TwitterException : " + ex.getMessage()
					//ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
					return
				
				} catch(Exception ex) { 
					Writer wr = new StringWriter();
					PrintWriter pWriter = new PrintWriter(wr);
					ex.printStackTrace(pWriter);
					SendEmail.sendErrorMail(wr.toString());
					ex.getMessage()
				}
		}
		println "Deleted followers: " + deletedFollowersMap.size()
		SendEmail.sendSuccessMail("delete followers", deletedFollowersMap.size());
	}

}