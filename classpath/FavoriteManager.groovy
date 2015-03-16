
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
import groovy.json.*

/**
 *
 */
class FavoriteManager {

	Twitter twitter
	def appDatasXml
		
	def userName
	def today = Calendar.instance

    FavoriteManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
				
		this.userName = appDatasXml.conf.user.@screenName.text()
    }

	def startAddFavoriteTweets(maxNewFavorites){
		final Map queryWordsMap = DataManager.getQueryWords(appDatasXml)
		
		final Map favoriteTweetsMap = new HashMap()
		def countNewFavoriteTweetGlobal = 0
        try {
		
			Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			RateLimitStatus rateLimitSearchTweet = RateUtil.checkRateLimitSearchTweet(rateLimitStatusMap, twitter)
				
			def splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
			
			def countWhile = 0
			while(countWhile < maxNewFavorites){
				println "----------------------------------------------------------------"
				countWhile++
				if(favoriteTweetsMap.size() == maxNewFavorites){
					continue;
				}
				queryWordsMap.each { queryWord, enabled ->
					println "------------------------------------------------"
					println "Start search favorite with '" + queryWord + "', enabled: " + enabled 	
					if(enabled){
						Query query = new Query(queryWord);
						query.setCount(150)
						QueryResult result = twitter.search(query);
						List<Status> tweets = result.getTweets();
						println "Query result for '" + queryWord + "', size: " + tweets.size() + ", splitByWord:" + splitByWord + ", maxNewFavorites:" + maxNewFavorites + ", favoriteTweetsMap: " + favoriteTweetsMap.size()
						if(tweets.size() == 0){
							queryWordsMap.put(queryWord, false)
							splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
							println "Disable for this run, Query word '$queryWord'"
						}
						def countNewFavoriteTweetByWord = 0
						def validTimeStamp = today.time.time - (45 * 60 * 1000)
						boolean nothingToAdd = true
						for (Status tweet : tweets) {
							println "--------------------------------"
							if(countNewFavoriteTweetByWord >= splitByWord){
								break
							}
							if(countNewFavoriteTweetGlobal >= maxNewFavorites){
								break
							}
							String tweetIdString = "" + tweet.getId()
							Date createdAt = tweet.getCreatedAt()
							println "Check Tweet, id: " + tweetIdString + ", date: " + createdAt.time + ", must be > " + validTimeStamp							
							if(!favoriteTweetsMap.containsKey(tweetIdString) && createdAt.time > validTimeStamp){
								println "Add favorite, id: " + tweetIdString + ", date: " + createdAt
								favoriteTweetsMap.put(tweetIdString, tweet.getId())
								countNewFavoriteTweetGlobal++
								countNewFavoriteTweetByWord++
								nothingToAdd = false
							}
						}
						if(nothingToAdd){
							queryWordsMap.put(queryWord, false)
							splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
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
		favoriteTweetsMap.each { key, value ->
			try {
				twitter.createFavorite(value)
				println "Create Favorite : " + value
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
		SendEmail.sendSuccessMail("add favorite", countNewFavoriteTweetGlobal);
	}

	def getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites){
		def countQueryWordEnabled = 0
		queryWordsMap.each { word, enabled ->
			if(enabled)
				countQueryWordEnabled++
		}
		if(countQueryWordEnabled != 0){
			def splitByWordResult = (maxNewFavorites / countQueryWordEnabled)
			def splitByWord = splitByWordResult.setScale(0, BigDecimal.ROUND_UP)
			return splitByWord;
		}
		return 2;
	}
	
	def cleanFavoriteTweets(){
	
		Map<String ,RateLimitStatus> rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
		RateLimitStatus rateLimitSearchTweet = RateUtil.checkRateLimitSearchTweet(rateLimitStatusMap, twitter)
		
		final Map queryWordsMap = DataManager.getQueryWords(appDatasXml)
		int page = 10
		int totalTweets = page * 20
		Paging paging = new Paging(1, totalTweets)
		ResponseList<Status> list = twitter.getFavorites(paging);
		def countFavoriteDeleted = 0
		def countFavoriteLoop = 0
		for (Iterator<Status> iterator = list.iterator(); iterator.hasNext();) {
			Status status = (Status) iterator.next();
			Date createdAt = status.getCreatedAt()
			def text = status.getText().toLowerCase()
			def validTimeStamp = today.time.time - (60 * 60 * 1000)
			countFavoriteLoop++
			println "createdAt: " + createdAt.time + ", validTimeStamp: " + validTimeStamp + ", contains QueryWords: " + containsQueryWord(queryWordsMap, text)
			if(createdAt.time <= validTimeStamp && containsQueryWord(queryWordsMap, text)){
				try {
					twitter.destroyFavorite(status.getId())
					countFavoriteDeleted++
					println "Delete Favorite tweet: '" + status.getId() + "', date: " + createdAt
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
			}
		}
		println "Favorite tweets deleted: " + countFavoriteDeleted + " on " + countFavoriteLoop
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