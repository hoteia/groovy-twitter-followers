
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
import groovy.json.*

/**
 *
 */
class PokeManager {

	Twitter twitter
	def appDatasXml
	
	long[] myFollowers
	def userName
	def today = Calendar.instance

    PokeManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
    }

	def startAddPokeTweets(maxNewPokes){
		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
		
		final Map historicMap = DataManager.getHistoryPokeTweets(appDatasXml);
		final Map queryWordsMap = DataManager.getQueryWords(appDatasXml)
		final Map pokeTweetsMap = new HashMap()
		def countNewPokeTweetGlobal = 0
        try {
			Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			RateLimitStatus rateLimitSearchTweet = RateUtil.checkRateLimitSearchTweetrateLimitStatusMap, twitter)
				
			def splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
			
			def countWhile = 0
			while(countWhile < maxNewPokes){
				println "----------------------------------------------------------------"
				countWhile++
				if(pokeTweetsMap.size() == maxNewPokes){
					continue;
				}
				queryWordsMap.each { queryWord, enabled ->
					println "------------------------------------------------"
					println "Start search poke with '" + queryWord + "', enabled: " + enabled 	
					if(enabled){
						Query query = new Query(queryWord);
						query.setCount(150)
						QueryResult result = twitter.search(query);
						List<Status> tweets = result.getTweets();
						println "Query result for '" + queryWord + "', size: " + tweets.size() + ", splitByWord:" + splitByWord + ", maxNewPokes:" + maxNewPokes + ", pokeTweetsMap: " + pokeTweetsMap.size()
						if(tweets.size() == 0){
							queryWordsMap.put(queryWord, false)
							splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
							println "Disable for this run, Query word '$queryWord'"
						}
						def countNewPokeTweetByWord = 0
						def validTimeStamp = today.time.time - (45 * 60 * 1000)
						boolean nothingToAdd = true
						for (Status tweet : tweets) {
							println "--------------------------------"
							if(countNewPokeTweetByWord >= splitByWord){
								break
							}
							if(countNewPokeTweetGlobal >= maxNewPokes){
								break
							}
							String tweetUserScreenName = "" + tweet.getUser().getScreenName()
							Date createdAt = tweet.getCreatedAt()
							println "Check Tweet, user id: " + tweetUserScreenName + ", date: " + createdAt.time + ", must be > " + validTimeStamp							
							if(!historicMap.containsKey(followerIdString) && !pokeTweetsMap.containsKey(tweetUserScreenName) && createdAt.time > validTimeStamp){
								def tweetUserId = tweet.getUser().getId()
								boolean isNotFollowingMe = ScriptGroovyUtil.isNotFollowingMe(this.myFollowers, tweetUserId)
								Relationship relationship = twitter.showFriendship(twitter.getId(), tweetUserId)
								println "isSourceFollowedByTarget: " + relationship.isSourceFollowedByTarget() + ", isNotFollowingMe: " + isNotFollowingMe
								if(!relationship.isSourceFollowedByTarget() && isNotFollowingMe){
									println "Add poke, id: " + tweetUserScreenName + ", date: " + createdAt
									pokeTweetsMap.put(tweetUserScreenName, tweet.getUser().getId())
									countNewPokeTweetGlobal++
									countNewPokeTweetByWord++
									nothingToAdd = false
								}
							}
						}
						if(nothingToAdd){
							queryWordsMap.put(queryWord, false)
							splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
							println "Disable for this run, Query word '$queryWord'"
						}
					}
				}
			}

		} catch (TwitterException ex) {
			println "TwitterException : " + ex.getMessage()
			
			Writer wr = new StringWriter();
			PrintWriter pWriter = new PrintWriter(wr);
			ex.printStackTrace(pWriter);
			SendEmail.sendErrorMail(wr.toString());
				
			if(ex.getRateLimitStatus() != null){
				ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
			}
        }
		
		def countPokeTweet = 0
		pokeTweetsMap.each { key, value ->
			try {
				def pokeStatus = "@$key follow us #poke"
				Status status = twitter.updateStatus(pokeStatus)
				println "Create Poke to: @" + key
				countPokeTweet++
			} catch (TwitterException ex) {
				println "TwitterException : '" + value + "' : " + ex.getMessage()
								
				//Writer wr = new StringWriter();
				//PrintWriter pWriter = new PrintWriter(wr);
				//ex.printStackTrace(pWriter);
				//SendEmail.sendErrorMail(wr.toString());
					
				if(ex.getRateLimitStatus() != null){
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
				}
			}
		}		
	
		println "Poke tweets added: " + countPokeTweet + " on " + pokeTweetsMap.size()
	
		// SAVE HISTORY
		// rewrite all the map : delete/write
		println "saved user in the history list: " + pokeTweetsMap.size()
		File file = new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/history_poke_tweets.properties');
		pokeTweetsMap.each { key, value ->
			try{
				 file << (key + "=" + value + "\n")
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			}
		}
		
		SendEmail.sendSuccessMail("add poke", countNewPokeTweetGlobal);
	}

	def getPokeTweetByQueryWord(queryWordsMap, maxNewPokes){
		def countQueryWordEnabled = 0
		queryWordsMap.each { word, enabled ->
			if(enabled)
				countQueryWordEnabled++
		}
		def splitByWordResult = (maxNewPokes / countQueryWordEnabled)
		def splitByWord = splitByWordResult.setScale(0, BigDecimal.ROUND_UP)
		return splitByWord;
	}
	
	def cleanPokeTweets(){
	
		Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
		RateLimitStatus rateLimitSearchTweet = RateUtil.checkRateLimitSearchTweet(rateLimitStatusMap, twitter)
		
		Paging paging = new Paging(1, 100)
		List<Status> statuses = twitter.getUserTimeline(paging);
		println "HomeTimeline: " + statuses.size()
		def twitterId = twitter.getId()
		def countPokeDeleted = 0
		def countPokeLoop = 0
		for (Status status : statuses) {
			countPokeLoop++
			def text = status.getText().toLowerCase();
			if(status.getUser().getId() == twitterId){
				println "My tweet: " + text
				if(text.contains("poke") && !text.contains("keep")){
					println "is poke tweet, destroy it"
					twitter.destroyStatus(status.getId())
					countPokeDeleted++
				}
			}
		}
		println "Poke tweets deleted: " + countPokeDeleted + " on " + countPokeLoop
	}

	boolean containsQueryWord(queryWordsMap, text){
		for ( e in queryWordsMap ) {
			if(text.toLowerCase().contains(e.key.toLowerCase())){
				return true;
			}
		}
		return false;
	}
	
}