
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import groovy.json.*

/**
 *
 */
class SourceProspectManager {

	Twitter twitter
	def appDatasXml

	def userName
	
    SourceProspectManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
    }

	def isNotUpToDate(){
		final Map sourceProspectsMap = DataManager.getSourceProspects(appDatasXml);
		if(sourceProspectsMap.size() > 0){
			final Map draftProspectsMap = DataManager.getDraftProspects(appDatasXml);
			if(draftProspectsMap.size() > 0){
				println "SOURCE PROSPECT LIST IS INITIALIZED - BUT SOME NEW TARGETS NEED TO BE EVALUATE"
				return true;
			}
			return false;
		} else {
			println "SOURCE PROSPECT LIST IS NOT INITIALIZED"
			return true;
		}
	}
	
	def initialize(){
		final Map sourceProspectsMap = DataManager.getSourceProspects(appDatasXml);
		final Map newSourceProspectsMap = new HashMap()
		final Map draftProspectsMap = DataManager.getDraftProspects(appDatasXml)
		if(draftProspectsMap.size() > 0){
		
			Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
			
			def countCallTwitterShowUser = 0
			draftProspectsMap.each { key, value ->
				try{
					// CHECK RATIO
					println "CHECK RATIO : " + rateLimitUserShow.getRemaining() + " : " + countCallTwitterShowUser 
					if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
						ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())	
						rateLimitUserShow = RateUtil.checkRateLimitUserShow(rateLimitStatusMap, twitter)
						countCallTwitterShowUser = 0
					}

					User user = twitter.showUser(key)
					String userIdString = "" + user.getId();
					countCallTwitterShowUser++
					if(!sourceProspectsMap.containsKey(userIdString)){
						def twitterUser = new TwitterUser( id: user.getId(), name: user.getName(), screenName: user.getScreenName(), createdAt: user.getCreatedAt(), favouritesCount: user.getFavouritesCount(), friendsCount: user.getFriendsCount(), followersCount: user.getFollowersCount())
						newSourceProspectsMap.put(userIdString, twitterUser);
					}
				} catch(TwitterException ex) { 
					println "TwitterException : " + ex.getMessage()
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset());
				} catch(IOException ex) { 
					System.err.println("An IOException was caught!");
					ex.printStackTrace();
				}
			}
			
			// SAVE/ADD NEW PROSPECT
			// rewrite all the map : delete/write
			File file = new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/friends_prospects.properties');
			println "prospect to write in the prospect source list: " + newSourceProspectsMap.entrySet().size()
			newSourceProspectsMap.each { key, value ->
				try{
					 file << (key + "=" + new JsonBuilder(value).toString() + "\n")
				} catch(IOException ex) { 
					System.err.println("An IOException was caught!"); 
					ex.printStackTrace(); 
				}
			}

			// clean draft			
			def draftProspectsFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/draft_prospects.properties');
			draftProspectsFile.delete()
			draftProspectsFile.createNewFile()  
			
		} else {
			println "DRAFT SOURCE PROSPECT LIST IS EMPTY - WE CAN'T CONTINUE"
		}		
	}
		
}