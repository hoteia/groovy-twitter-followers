
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
import groovy.json.*

/**
 *
 */
class FollowerManager {

	Twitter twitter
	long[] myFollowers
	def appDatasXml
	
	def userName
	def myUserId
	
	def today = Calendar.instance
							
    FollowerManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
		this.myUserId = appDatasXml.conf.user.@id.text()
    }

	def initFollowersListToCheck(Map sourceProspectsMap, int maxNewFollowers){

		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
	
		final Map historicMap = DataManager.getHistoricIds(appDatasXml);
		final Map ignorefollowersMap = DataManager.getIgnoreFollowers(appDatasXml);
		final Map blockedByUsersMap = DataManager.getBlockedByUsers(appDatasXml)
		final Map targetedFollowersMap = new HashMap();
		final Map followersOutProspectMap = new HashMap();

		def countSourceProspectNotDryOut = 0
		sourceProspectsMap.each { key, twitterUserProspectJson ->
			if(!twitterUserProspectJson.followDryOut)
				countSourceProspectNotDryOut++
		}
		def splitByProspectResult = (maxNewFollowers / countSourceProspectNotDryOut)
		def splitByProspect = splitByProspectResult.setScale(0, BigDecimal.ROUND_UP)
		println "maxNewFollowers: " + maxNewFollowers + ", sourceProspectsMap size:" + sourceProspectsMap.size() + ", countSourceProspectNotDryOut :" + countSourceProspectNotDryOut + ", splitByProspect:" + splitByProspectResult + "/" + splitByProspect
		if(countSourceProspectNotDryOut > 0){
			println "INIT FOLLOWERS LIST TO CHECK IN HISTORY FILE"
			
			println "----------------------------------------------------------------"
			
			sourceProspectsMap.each { key, twitterUserProspectJson ->
				println "------------------------------------------------"
				if(targetedFollowersMap.size() >= maxNewFollowers){
					return
				}
				if(!followersOutProspectMap.containsKey(key) && !twitterUserProspectJson.followDryOut){

					try{
						long prospectTwitterId = new Long(key).longValue();

						Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
						RateLimitStatus rateLimitStatusFollowers = RateUtil.checkRateLimitStatusFollowers(rateLimitStatusMap, twitter)
						RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)

						boolean continueItIdsPagination = true;
						long cursor = -1;
						if(twitterUserProspectJson.followLastCursor != 0){
							cursor = twitterUserProspectJson.followLastCursor;
						}
						
						def countCallTwitterStatusFollowers = 0;
						def countCallTwitterShowUser = 0;
						def countUserToAddByProspect = 0;
						
						while(continueItIdsPagination){
							// CHECK RATIO
							println "-------------------------------------------------------"
							println "CHECK RATIO : Remaining = " + rateLimitStatusFollowers.getRemaining() + " : StatusFollowers = " + countCallTwitterStatusFollowers + " : targetedFollowersMap = " + targetedFollowersMap.size()
							if(countCallTwitterStatusFollowers >= (rateLimitStatusFollowers.getRemaining() - 1)){
								rateLimitStatusFollowers = RateUtil.checkRateLimitStatusFollowers(null, twitter)
								countCallTwitterStatusFollowers = 0
							}
							println "-------------------------------------------------------"

							IDs followerIds = twitter.getFollowersIDs(prospectTwitterId, cursor);
							println "******************************************************"
							println "### Target: $key/$twitterUserProspectJson.screenName"
							println "### Page/cursor: $cursor"
							println "### Followers: " + followerIds.getIDs().length
							println "******************************************************"
							long[] longIds = followerIds.getIDs();

							rateLimitUserShow = RateUtil.checkRateLimitUserShow(null, twitter)
							
							for (int i = 0; i < longIds.length; i++) {
								if(targetedFollowersMap.size() >= maxNewFollowers){
									println "targetedFollowersMap.size() >= maxNewFollowers"
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
								if(!historicMap.containsKey(followerIdString) && !ignorefollowersMap.containsKey(followerIdString) && !followerIdString.equals(myUserId)){
									try {
									
										// CHECK RATIO
										println "-------------------------------------------------------"
										println "CHECK RATIO : Remaining = " + rateLimitUserShow.getRemaining() + " : ShowUser = " + countCallTwitterShowUser + " : targetedFollowersMap = " + targetedFollowersMap.size()
										if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
											println "- Reset UserShow Rate"
											rateLimitUserShow = RateUtil.checkRateLimitUserShow(null, twitter)
											countCallTwitterShowUser = 0
										}
										println "-------------------------------------------------------"

										User showUser = twitter.showUser(followerId);
										println "### User info: " + showUser.getScreenName() + "/" + showUser.getName() 
										countCallTwitterShowUser++
										Status status = showUser.getStatus();
										int userFollowersCount = showUser.getFollowersCount()
										def followRequestSent = showUser.isFollowRequestSent()
										boolean isNotFollowingMe = ScriptGroovyUtil.isNotFollowingMe(this.myFollowers, followerId)
										
										println "### Followers: " + userFollowersCount + " / follow request sent: " + followRequestSent + " / test isNotFollowingMe: "+ isNotFollowingMe + " / status not null: " + (status != null)
										println "### Test isNotFollowingMe: " + isNotFollowingMe + " / Status is not null: " + (status != null)
										// && userFollowersCount < 2000 
										if(!showUser.isFollowRequestSent() && isNotFollowingMe && status != null){
											Relationship relationship = twitter.showFriendship(twitter.getId(), showUser.getId())
											println "### isSourceFollowedByTarget: " + relationship.isSourceFollowedByTarget()
											if(!relationship.isSourceFollowedByTarget()){
												def validDays = 2
												Date lastStatusDate = showUser.getStatus().getCreatedAt();
												def validDateTime = today.time - validDays
												def ignoreDateTime = today.time - 30
												println "Last statut is recent < $validDays days: " + (lastStatusDate.time >= validDateTime.time) + " -> " +lastStatusDate
												println "Last statut is not recent >30, ignore: " + (lastStatusDate.time <= validDateTime.time) + " -> " +lastStatusDate
												
												if(lastStatusDate.time >= validDateTime.time){
													def twitterUser = new TwitterUser( id: showUser.getId(), name: showUser.getName(), screenName: showUser.getScreenName(), createdAt: showUser.getCreatedAt(), favouritesCount: showUser.getFavouritesCount(), friendsCount: showUser.getFriendsCount(), followersCount: showUser.getFollowersCount(), followDryOut:false)
													targetedFollowersMap.put(showUser.getId(), twitterUser)
													println "- Keep - Total users to add: " + targetedFollowersMap.size()
													countUserToAddByProspect++
												} else if (lastStatusDate.time <= ignoreDateTime.time){
													def twitterUserInfo = showUser.getScreenName() + ";" + showUser.getName();
													ignorefollowersMap.put(showUser.getId(), twitterUserInfo)
													println "- Ignore - ignore followers Map size:  " + ignorefollowersMap.size()
												}
											}
										} else {
											println "- Skip -"
										}
										
									} catch(TwitterException ex) { 
										println "TwitterException : " + ex.getMessage()
										Map<String ,RateLimitStatus> rateLimitStatusMapError = RateUtil.checkRateLimit(null, twitter)
										for (String endpoint : rateLimitStatusMapError.keySet()) {
											RateLimitStatus status = rateLimitStatusMapError.get(endpoint);
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
											rateLimitUserShow = RateUtil.checkRateLimitUserShow(null, twitter)
										}
									}
								} else {
									println "- User is not eligible -"
									if(historicMap.containsKey(followerIdString)){
										println "historicMap containsKey $followerIdString: " + historicMap.containsKey(followerIdString)
									}
									if(ignorefollowersMap.containsKey(followerIdString)){
										println "ignorefollowersMap containsKey $followerIdString: " + ignorefollowersMap.containsKey(followerIdString)
									}
									if(followerIdString.equals(myUserId)){
										println "It's me!! followerIdString.equals(myUserId): " + myUserId
									}
								}
								println "******************************************************"
							}
							
							
							cursor = followerIds.getNextCursor();
							twitterUserProspectJson.followLastCursor = cursor
							if(!followerIds.hasNext()){
								println "No more followers for $twitterUserProspectJson.screenName"
								continueItIdsPagination = false
								twitterUserProspectJson.followDryOut = true
								sourceProspectsMap.put(key, twitterUserProspectJson)
								followersOutProspectMap.put(key, key)
							}
							countCallTwitterStatusFollowers++
						}


						
					} catch(TwitterException ex) { 
						println "TwitterException : " + ex.getMessage()
						Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
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
						ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
					} catch(IOException ex) { 
						System.err.println("An IOException was caught!")
						ex.printStackTrace(); 
					}
				
				} else {
					println "Skip $key/$twitterUserProspectJson.screenName (dry out: $twitterUserProspectJson.followDryOut) followers out!"
				}
			}	
			println "targetedFollowersMap : " + targetedFollowersMap.size()

		} else {
			println "SOURCE PROSPECT LIST IS NOT INITIALIZED"
		}
		
		
		// SAVE IGNORE FOLLOWER
		// rewrite all the map : delete/write
		println "added users to write in the ignore list: " + ignorefollowersMap.entrySet().size()
		File file = new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/ignore_followers.properties');
		ignorefollowersMap.each { key, value ->
			try{
				 file << (key + "=" + value + "\n")
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			}
		}
		
		
		
		// rewrite all the map : delete/write
		FileWriter fstream = new FileWriter(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/friends_prospects.properties');
		BufferedWriter bufferedWriter = new BufferedWriter(fstream); 
		println "friends to write in the new friend prospect list: " + sourceProspectsMap.entrySet().size()
		sourceProspectsMap.each { key, value ->
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


		
		return targetedFollowersMap;
	}
	
	def addFollowers(Map targetedFollowersMap){
		final Map addedFollowersMap = new HashMap()
		final Map blockUsersMap = new HashMap()
		targetedFollowersMap.each { key, value ->
			try{
				long prospectTwitterId = new Long(key).longValue();
				addedFollowersMap.put(key, value)
				twitter.createFriendship(prospectTwitterId)
				println "Add follower $key/$value.screenName"
				
			} catch(TwitterException ex) { 
				addedFollowersMap.put(key, value)

				Writer wr = new StringWriter();
				PrintWriter pWriter = new PrintWriter(wr);
				ex.printStackTrace(pWriter);
				def errorMessage = "Error to add follower $key/$value.screenName" + wr.toString()
				SendEmail.sendErrorMail(errorMessage);
				println "TwitterException: " + ex.getErrorCode() + " : " + ex.getStatusCode() + " : " + ex.getMessage()
				
				def errorCode = ex.getErrorCode()
				if(162 == errorCode){
					blockUsersMap.put(key, value)
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
		println "added friend to write in the history list: " + addedFollowersMap.size()
		File file = new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/history_add_followers.properties');
		addedFollowersMap.each { key, value ->
			try{
				file << (key + "=" + new JsonBuilder(value).toString() + "\n")
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
		final Map historicMap = DataManager.getHistoricIds(appDatasXml);
		final Map targetedFollowersMap = new HashMap();
		int page = 1;
		def ignoreDateTime = today.time - 30
		while(page < 20){
			ResponseList<User> users = twitter.searchUsers(word, page);
			for (User user : users) {
				if(targetedFollowersMap.size() >= maxNewFollowers){
					break;
				}
				String myUserIdString = "" + user.getId()
				if(!historicMap.containsKey(myUserIdString)){
					if (null != user.getStatus()) {
						def validDays = 2
						Date lastStatusDate = user.getStatus().getCreatedAt();
						def validDateTime = today.time - validDays
						if(lastStatusDate.time >= validDateTime.time){
							targetedFollowersMap.put(user.getId(), user)
							println "Keep this User, total: " + targetedFollowersMap.size()
						} 
					}
				}
			}
			page++;
		}
		return targetedFollowersMap;
	}
	
	def cleanFollowers(String word){
	
		Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
		RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)

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
						rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
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