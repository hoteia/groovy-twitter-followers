
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
	def rootScriptDir = System.getProperty("rootScriptDir")
	def userName
	def userId
	def today = Calendar.instance
							
    FollowerManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.userName = appDatasXml.conf.user.@screenName.text()
		this.userId = appDatasXml.conf.user.@id.text()
		def userFolder = new File(rootScriptDir + 'datas/' + userName + '/')
		if(!userFolder.exists()){
			userFolder.mkdirs()  
		}
    }

	def initFollowersListToCheck(Map sourceProspectsMap, int maxNewFollowers){

		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
	
		final Map historicMap = getHistoricIds();
		final Map targetedfollowersMap = new HashMap();
		final Map ignorefollowersMap = getIgnoreFollowers();
		def secureMaxWhileCountIt = 10
		def splitByProspectResult = (maxNewFollowers / sourceProspectsMap.size())
		def splitByProspect = splitByProspectResult.setScale(0, BigDecimal.ROUND_UP)
		println "maxNewFollowers: " + maxNewFollowers + ", sourceProspectsMap size:" + sourceProspectsMap.size() + ", splitByProspect:" + splitByProspectResult + "/" + splitByProspect
		if(sourceProspectsMap.size() > 0){
			println "INIT FOLLOWERS LIST TO CHECK IN HISTORY FILE"
			def countWhileIt = 0;
			while(countWhileIt < secureMaxWhileCountIt){
				println "countWhileIt: $countWhileIt | secureMaxWhileCountIt: $secureMaxWhileCountIt"
				sourceProspectsMap.each { key, value ->
					if(targetedfollowersMap.size() >= maxNewFollowers){
						return
					}
					try{
						long prospectTwitterId = new Long(key).longValue();
						
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

RateLimitStatus rateLimitStatusFollowers = rateLimitStatusMap.get("/followers/ids")
System.out.println("Followers Limit: " + rateLimitStatusFollowers.getLimit());
System.out.println("Followers Remaining: " + rateLimitStatusFollowers.getRemaining());
System.out.println("Followers SecondsUntilReset: " + rateLimitStatusFollowers.getSecondsUntilReset());
if(rateLimitStatusFollowers.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusFollowers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusFollowers = rateLimitStatusMap.get("/followers/ids");
}

RateLimitStatus rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
System.out.println("UsersShow Limit: " + rateLimitStatusUsers.getLimit());
System.out.println("UsersShow Remaining: " + rateLimitStatusUsers.getRemaining());
//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusUsers.getResetTimeInSeconds());
System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusUsers.getSecondsUntilReset());
if(rateLimitStatusUsers.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
}

						IDs followerIds = twitter.getFollowersIDs(prospectTwitterId, -1)
						println followerIds.getIDs().length + " followers for $key/$value"

						long[] longIds = followerIds.getIDs();
						def countCallTwitterShowUser = 0;
						def countUserToAddByProspect = 0;
						for (int i = 0; i < longIds.length; i++) {
							if(targetedfollowersMap.size() >= maxNewFollowers){
								break;
							}
							if(countUserToAddByProspect >= splitByProspect){
								break;
							}
							
							long followerId = longIds[i];
							String followerIdString = "" + followerId;
							if(!historicMap.containsKey(followerIdString) && !ignorefollowersMap.containsKey(followerIdString) && !followerIdString.equals(userId)){
								try {
								
// CHECK RATIO
println "CHECK RATIO : Remaining = " + rateLimitStatusUsers.getRemaining() + " : ShowUser = " + countCallTwitterShowUser + " : targetedfollowersMap = " + targetedfollowersMap.size()
if(countCallTwitterShowUser >= (rateLimitStatusUsers.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
	countCallTwitterShowUser = 0
}

									User user = twitter.showUser(followerId);
									countCallTwitterShowUser++
									Status status = user.getStatus();
									int userFollowersCount = user.getFollowersCount()
									def followRequestSent = user.isFollowRequestSent()
									println "User : $followerId/" + user.getScreenName() + "/" + user.getName() + "/friends: " + userFollowersCount + " / follow requestsent: " + followRequestSent + " / test isNotFollowingMe: "+ isNotFollowingMe(followerId) + " / status not null: " + (status != null)
									if(!user.isFollowRequestSent() && isNotFollowingMe(followerId)  && userFollowersCount < 2000 && status != null){
										Relationship relationship = twitter.showFriendship(twitter.getId(), user.getId())
										println "isSourceFollowedByTarget: " + relationship.isSourceFollowedByTarget()
										if(!relationship.isSourceFollowedByTarget()){
											def validDays = 2
											Date lastStatusDate = user.getStatus().getCreatedAt();
											def validDateTime = today.time - validDays
											def ignoreDateTime = today.time - 30
											println "Last statut is recent < $validDays days: " + (lastStatusDate.time >= validDateTime.time) + " -> " +lastStatusDate
											println "Last statut is not recent >30, ignore: " + (lastStatusDate.time <= validDateTime.time) + " -> " +lastStatusDate
											
											if(lastStatusDate.time >= validDateTime.time){
												targetedfollowersMap.put(user.getId(), user)
												println "Keep this User, total: " + targetedfollowersMap.size()
												countUserToAddByProspect++
											} else if (lastStatusDate.time <= ignoreDateTime.time){
												def twitterUserInfo = user.getScreenName() + ";" + user.getName();
												ignorefollowersMap.put(user.getId(), twitterUserInfo)
												println "Ignore this User, total: " + ignorefollowersMap.size()
											}
										}
									}
									
								} catch(TwitterException ex) { 
									println "TwitterException : " + ex.getMessage()
									if(ex.getRateLimitStatus() != null){
										ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
									}
								}
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
				countWhileIt++
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
		targetedfollowersMap.each { key, value ->
			def twitterUserInfo = value.getScreenName() + ";" + value.getName();
			try{
				long prospectTwitterId = new Long(key).longValue();
				twitter.createFriendship(prospectTwitterId)
				println "Add follower $key/$twitterUserInfo"
				addedFollowersMap.put(key, twitterUserInfo)
				
			} catch(TwitterException ex) { 
				Writer wr = new StringWriter();
				PrintWriter pWriter = new PrintWriter(wr);
				ex.printStackTrace(pWriter);
				def errorMessage = "Error to add follower $key/$twitterUserInfo" + wr.toString()
				SendEmail.sendErrorMail(errorMessage);
				println "TwitterException : " + ex.getMessage()
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
	
	def getHistoricIds(){
		final Map historicMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(rootScriptDir + 'datas/' + userName + '/history_add_followers.properties');
			if (!protectedFollowingFile.exists()) {
				protectedFollowingFile.createNewFile()  
				return historicMap;
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
						historicMap.put(twitterUserId, twitterUserScreenName);
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
		return historicMap;
	}
	
	def getIgnoreFollowers(){
		final Map ignoreFollowersMap = new HashMap()
		try {
			def ignoreFollowersFile =  new File(rootScriptDir + 'datas/' + userName + '/ignore_followers.properties');
			if (!ignoreFollowersFile.exists()) {
				ignoreFollowersFile.createNewFile()  
				return ignoreFollowersMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(ignoreFollowersFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def twitterUserScreenName = split[1].split(";");
						ignoreFollowersMap.put(twitterUserId, split[1]);
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
		return ignoreFollowersMap;
	}
	
	def isNotFollowingMe(long targetFollowerId){
		for (int i = 0; i < this.myFollowers.length; i++) {
			long followerId = this.myFollowers[i];
			if(followerId == targetFollowerId){
				return false;
			}
		}
		return true
	}
	
	
	
	
	
	
	
	
	def searchFollowersToAdd(String word, int maxNewFollowers){

		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
		final Map historicMap = getHistoricIds();
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

RateLimitStatus rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
System.out.println("UsersShow Limit: " + rateLimitStatusUsers.getLimit());
System.out.println("UsersShow Remaining: " + rateLimitStatusUsers.getRemaining());
//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusUsers.getResetTimeInSeconds());
System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusUsers.getSecondsUntilReset());
if(rateLimitStatusUsers.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
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
println "CHECK RATIO : Remaining = " + rateLimitStatusUsers.getRemaining()  + ", countCallTwitterShowUser:" + countCallTwitterShowUser + ", deletedFollowersMap: " + deletedFollowersMap.size()
if(countCallTwitterShowUser >= (rateLimitStatusUsers.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
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